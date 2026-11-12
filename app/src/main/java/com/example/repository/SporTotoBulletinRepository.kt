package com.example.repository

import android.content.Context
import com.example.BuildConfig
import com.example.model.MatchLiveScore
import com.example.model.MatchStatus
import com.example.model.TotoMatch
import com.example.network.NetworkModule
import com.example.network.ApiFootballLiveService
import com.example.network.ScoreCardsLiveService
import com.example.network.SporTotoApiService
import com.example.network.model.BulletinMatchDto
import com.example.network.model.WeeklyBulletinResponse
import com.example.network.model.OfficialGameRound
import com.example.network.model.OfficialRoundMatch
import com.example.network.model.MatchProbabilitiesDto
import com.example.network.model.toDomainTotoMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Repository interface for managing weekly Spor Toto bulletin, matches, dates, live odds, and real-time scores.
 */
interface SporTotoBulletinRepository {
    val cachedBulletin: StateFlow<WeeklyBulletinResponse?>
    val liveScores: StateFlow<List<MatchLiveScore>>
    val liveScoreProvider: StateFlow<String>

    /** Free API-Football settings. Implementations used by tests may omit these. */
    fun hasApiFootballKey(): Boolean = false
    fun configureApiFootballKey(key: String) {}
    fun freeScoreRequestsRemaining(): Int? = null

    /**
     * Fetches current weekly bulletin from external API (or returns cached if available).
     */
    suspend fun getWeeklyBulletin(forceRefresh: Boolean = false): Result<WeeklyBulletinResponse>

    /**
     * Fetches a specific weekly bulletin by week number.
     */
    suspend fun getBulletinByWeek(weekNumber: Int): Result<WeeklyBulletinResponse>

    /**
     * Refreshes real-time live betting odds for the current 15 matches.
     */
    suspend fun refreshLiveOdds(): Result<List<BulletinMatchDto>>

    /**
     * Polls the latest real-time match results and scores for the 15 matches.
     */
    suspend fun pollLiveScores(): Result<List<MatchLiveScore>>

    /**
     * Retrieves the 15 matches mapped to the domain TotoMatch model.
     */
    suspend fun getDomainMatches(forceRefresh: Boolean = false): Result<List<TotoMatch>>
}

/**
 * Concrete implementation of SporTotoBulletinRepository utilizing Retrofit.
 */
class SporTotoBulletinRepositoryImpl(
    private val apiService: SporTotoApiService = NetworkModule.apiService,
    private val liveScoreService: ApiFootballLiveService = NetworkModule.liveScoreService,
    private val liveScoreApiKey: String = BuildConfig.API_FOOTBALL_KEY,
    // Retained as constructor options for compatibility with older callers.
    private val scoreCardsService: ScoreCardsLiveService = NetworkModule.scoreCardsService,
    private val trackedLeagueIds: List<Int> = listOf(5, 39, 140, 135, 1),
    applicationContext: Context? = null
) : SporTotoBulletinRepository {

    companion object {
        /** Keep 20 of the Free plan's 100 daily calls in reserve. */
        const val FREE_DAILY_REQUEST_BUDGET = 80
        /** Free mode checks scores at most every three minutes per device. */
        const val FREE_MIN_POLL_INTERVAL_MS = 3 * 60 * 1000L
    }

    private val preferences = applicationContext?.applicationContext
        ?.getSharedPreferences("api_football_free", Context.MODE_PRIVATE)
    @Volatile private var configuredApiKey: String =
        preferences?.getString("api_key", null)?.trim().orEmpty()
            .ifBlank { liveScoreApiKey.trim() }
    @Volatile private var quotaDay: String = preferences?.getString("quota_day", "").orEmpty()
    @Volatile private var usedToday: Int = preferences?.getInt("used_today", 0) ?: 0
    @Volatile private var lastAttemptMs: Long = preferences?.getLong("last_attempt", 0L) ?: 0L
    @Volatile private var providerRemaining: Int? =
        preferences?.getInt("provider_remaining", -1)?.takeIf { it >= 0 }
    private var lastSuccessfulRequestMs = 0L
    private val pollMutex = Mutex()

    private val _cachedBulletin = MutableStateFlow<WeeklyBulletinResponse?>(null)
    override val cachedBulletin: StateFlow<WeeklyBulletinResponse?> = _cachedBulletin.asStateFlow()

    private val _liveScores = MutableStateFlow<List<MatchLiveScore>>(emptyList())
    override val liveScores: StateFlow<List<MatchLiveScore>> = _liveScores.asStateFlow()

    private val _liveScoreProvider = MutableStateFlow("API-FOOTBALL (Free)")
    override val liveScoreProvider: StateFlow<String> = _liveScoreProvider.asStateFlow()

    override fun hasApiFootballKey(): Boolean = configuredApiKey.isNotBlank()

    override fun configureApiFootballKey(key: String) {
        val normalized = key.trim()
        if (normalized != configuredApiKey) {
            configuredApiKey = normalized
            usedToday = 0
            lastAttemptMs = 0L
            lastSuccessfulRequestMs = 0L
            providerRemaining = null
            _liveScores.value = emptyList()
            preferences?.edit()
                ?.putString("api_key", normalized)
                ?.putInt("used_today", 0)
                ?.putLong("last_attempt", 0L)
                ?.putInt("provider_remaining", -1)
                ?.apply()
        }
    }

    override fun freeScoreRequestsRemaining(): Int? {
        if (!hasApiFootballKey()) return null
        val today = utcDay()
        val localRemaining = if (today == quotaDay) {
            (FREE_DAILY_REQUEST_BUDGET - usedToday).coerceAtLeast(0)
        } else FREE_DAILY_REQUEST_BUDGET
        return minOf(localRemaining, providerRemaining ?: 100)
    }

    private fun utcDay(): String = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())

    private fun persistQuota() {
        preferences?.edit()
            ?.putString("quota_day", quotaDay)
            ?.putInt("used_today", usedToday)
            ?.putLong("last_attempt", lastAttemptMs)
            ?.putInt("provider_remaining", providerRemaining ?: -1)
            ?.apply()
    }

    private fun refreshDailyQuota() {
        val today = utcDay()
        if (quotaDay != today) {
            quotaDay = today
            usedToday = 0
            lastAttemptMs = 0L
            providerRemaining = null
            persistQuota()
        }
    }

    /**
     * API-Football is the only live-score source. Free quota is tracked locally
     * and provider response headers are also honored. Scores and minutes are
     * taken verbatim from the authenticated API; no fallback or mock games.
     */
    override suspend fun pollLiveScores(): Result<List<MatchLiveScore>> =
        withContext(Dispatchers.IO) {
            pollMutex.withLock {
                fun unavailable(message: String): Result<List<MatchLiveScore>> {
                    _liveScores.value = emptyList()
                    return Result.failure(IllegalStateException(message))
                }

                val key = configuredApiKey
                if (key.isBlank()) {
                    return@withLock unavailable(
                        "Canlı skor için ücretsiz API-Football anahtarınızı girin."
                    )
                }
                refreshDailyQuota()
                val now = System.currentTimeMillis()
                if (lastAttemptMs > 0L && now - lastAttemptMs < FREE_MIN_POLL_INTERVAL_MS) {
                    return@withLock if (lastSuccessfulRequestMs == lastAttemptMs) {
                        Result.success(_liveScores.value)
                    } else {
                        unavailable("Ücretsiz kotayı korumak için 3 dakika bekleyip tekrar deneyin.")
                    }
                }
                if (usedToday >= FREE_DAILY_REQUEST_BUDGET || providerRemaining == 0) {
                    return@withLock unavailable(
                        "API-Football ücretsiz günlük sorgu sınırına ulaşıldı. " +
                            "Kota 00:00 UTC'de yenilenir."
                    )
                }

                // Count attempts too; invalid keys and HTTP errors can consume quota.
                lastAttemptMs = now
                usedToday++
                persistQuota()

                try {
                    val response = liveScoreService.getLiveFixtures(key)
                    response.headers()["x-ratelimit-requests-remaining"]
                        ?.toIntOrNull()?.let { providerRemaining = it.coerceAtLeast(0) }
                    persistQuota()
                    val body = response.body()
                    if (!response.isSuccessful || body == null) {
                        val problem = when (response.code()) {
                            401, 403 -> "API-Football anahtarı geçersiz veya erişim izni yok."
                            429 -> "API-Football ücretsiz istek kotası doldu."
                            else -> "API-Football yanıt vermedi (HTTP ${response.code()})."
                        }
                        return@withLock unavailable(problem)
                    }

                    val providerErrors = body.errors
                    if ((providerErrors is Map<*, *> && providerErrors.isNotEmpty()) ||
                        (providerErrors is List<*> && providerErrors.isNotEmpty())) {
                        return@withLock unavailable(
                            "API-Football isteği reddetti. Anahtarınızı ve ücretsiz plan kotanızı kontrol edin."
                        )
                    }
                    val actualScores = body.response.mapNotNull { item ->
                        val status = when (item.fixture.status.short.uppercase()) {
                            "HT" -> MatchStatus.HALF_TIME
                            "FT", "AET", "PEN" -> MatchStatus.FINISHED
                            "1H", "2H", "ET", "BT", "P", "LIVE", "INT", "SUSP" -> MatchStatus.LIVE
                            else -> MatchStatus.UPCOMING
                        }
                        if (status == MatchStatus.UPCOMING) return@mapNotNull null
                        MatchLiveScore(
                            matchNumber = item.fixture.id,
                            homeTeam = item.teams.home.name,
                            awayTeam = item.teams.away.name,
                            homeScore = item.goals.home,
                            awayScore = item.goals.away,
                            status = status,
                            minuteText = when {
                                status == MatchStatus.HALF_TIME -> "İY"
                                status == MatchStatus.FINISHED -> "MS"
                                item.fixture.status.elapsed != null ->
                                    "${item.fixture.status.elapsed}'"
                                else -> "—"
                            },
                            matchDate = item.fixture.date.orEmpty(),
                            league = item.league?.name.orEmpty()
                        )
                    }.distinctBy { it.matchNumber }
                        .sortedWith(compareByDescending<MatchLiveScore> { it.isLive }
                            .thenByDescending { it.matchDate })
                    _liveScoreProvider.value = "API-FOOTBALL (Free)"
                    _liveScores.value = actualScores
                    lastSuccessfulRequestMs = lastAttemptMs
                    Result.success(actualScores)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    unavailable("API-Football bağlantısı kurulamadı. İnterneti kontrol edin.")
                }
            }
        }

    private val officialTimezone = TimeZone.getTimeZone("Europe/Istanbul")

    private fun seasonCandidates(): List<String> {
        val calendar = Calendar.getInstance(officialTimezone)
        val year = calendar.get(Calendar.YEAR)
        val startingYear = if (calendar.get(Calendar.MONTH) < Calendar.JULY) year - 1 else year
        return listOf(
            "${startingYear}/${startingYear + 1}",
            "${startingYear - 1}/${startingYear}"
        )
    }

    private fun officialTimestamp(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (pattern in patterns) {
            val parser = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = officialTimezone
            }
            val parsed = runCatching { parser.parse(text.trim())?.time }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    private fun formatOfficialDate(raw: String?): String? {
        val timestamp = officialTimestamp(raw) ?: return null
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).apply {
            timeZone = officialTimezone
        }.format(Date(timestamp))
    }

    private fun officialRoundWeek(name: String?): Int =
        Regex("""(\d{1,2})\s*\.\s*(?:Oyun Programı|Hafta)""", RegexOption.IGNORE_CASE)
            .find(name.orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 0

    private fun validOfficialMatches(items: List<OfficialRoundMatch>): Boolean {
        if (items.size != 15) return false
        val keys = HashSet<String>()
        return items.all { item ->
            val fixture = item.match ?: return@all false
            val home = fixture.homeTeam?.name?.trim().orEmpty()
            val away = fixture.awayTeam?.name?.trim().orEmpty()
            val date = formatOfficialDate(fixture.date)
            home.isNotBlank() && away.isNotBlank() &&
                !home.equals(away, ignoreCase = true) && date != null &&
                keys.add("${home.uppercase(Locale.ROOT)}|${away.uppercase(Locale.ROOT)}|$date")
        }
    }

    private fun toOfficialBulletin(
        season: String,
        round: OfficialGameRound,
        items: List<OfficialRoundMatch>
    ): WeeklyBulletinResponse {
        val matches = items.mapIndexed { index, item ->
            val match = requireNotNull(item.match)
            BulletinMatchDto(
                matchNumber = index + 1,
                homeTeam = requireNotNull(match.homeTeam?.name).trim(),
                awayTeam = requireNotNull(match.awayTeam?.name).trim(),
                matchDate = requireNotNull(formatOfficialDate(match.date)),
                league = listOfNotNull(match.stage?.name, match.round?.name)
                    .filter { it.isNotBlank() }.joinToString(" • ").ifBlank { null },
                status = if (match.fullTimeWin != null || match.noterWin != null)
                    "FINISHED" else "UPCOMING",
                // The published program does not provide real 1/0/2 playing
                // percentages or decimal odds. Do not invent them.
                liveOdds = null,
                probabilities = null
            )
        }
        val dates = matches.map { it.matchDate }.sortedBy { m ->
            val parser = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).apply {
                timeZone = officialTimezone
            }
            runCatching { parser.parse(m)?.time }.getOrNull() ?: Long.MAX_VALUE
        }
        val title = round.name?.trim().takeUnless { it.isNullOrEmpty() }
            ?: items.firstOrNull()?.gameRoundName.orEmpty()
        val fetched = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).apply {
            timeZone = officialTimezone
        }.format(Date())
        return WeeklyBulletinResponse(
            weekNumber = officialRoundWeek(title),
            season = season,
            bulletinTitle = title,
            startDate = dates.first().substringBefore(" "),
            endDate = dates.last().substringBefore(" "),
            jackpotEstimateTL = 0.0,
            isActive = matches.any { it.status != "FINISHED" },
            matches = matches,
            sourceName = "Spor Toto Teşkilat Başkanlığı",
            fetchedAt = fetched
        )
    }

    private suspend fun fetchOfficialBulletin(requestedWeek: Int? = null): WeeklyBulletinResponse {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        var lastError: Throwable? = null

        for (season in seasonCandidates()) {
            val rounds = try {
                val response = apiService.getPublishedRounds(season)
                val body = response.body()
                if (!response.isSuccessful || body?.isSucceed != true) {
                    throw IllegalStateException("Resmî hafta listesi alınamadı (HTTP ${response.code()}).")
                }
                body.rounds.orEmpty().filter { (it.id ?: 0) > 0 }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                lastError = error
                continue
            }

            val candidates = rounds
                .distinctBy { it.id }
                .filter { round ->
                    if (requestedWeek != null) {
                        officialRoundWeek(round.name) == requestedWeek
                    } else {
                        val close = officialTimestamp(round.roundCloseDate)
                        close != null && close >= now - 7 * day && close <= now + 30 * day
                    }
                }
                .sortedWith(compareBy<OfficialGameRound> {
                    val close = officialTimestamp(it.roundCloseDate) ?: Long.MAX_VALUE
                    kotlin.math.abs(close - now)
                }.thenByDescending { it.id ?: 0 })
                .take(8)

            for (round in candidates) {
                val roundId = round.id ?: continue
                try {
                    val response = apiService.getRoundMatches(roundId)
                    val body = response.body()
                    if (!response.isSuccessful || body?.isSucceed != true) continue
                    val matches = body.items.orEmpty()
                    if (!validOfficialMatches(matches)) continue
                    val allCompleted = matches.all {
                        it.match?.fullTimeWin != null || it.match?.noterWin != null
                    }
                    if (requestedWeek == null && allCompleted) continue
                    return toOfficialBulletin(season, round, matches)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    lastError = error
                }
            }
        }
        throw IllegalStateException(
            if (requestedWeek == null) {
                "Resmî Spor Toto servisinde doğrulanmış, güncel 15 maçlık bülten bulunamadı."
            } else {
                "Resmî Spor Toto servisinde ${requestedWeek}. haftanın 15 maçlık listesi bulunamadı."
            },
            lastError
        )
    }

    override suspend fun getWeeklyBulletin(forceRefresh: Boolean): Result<WeeklyBulletinResponse> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                _cachedBulletin.value?.let { return@withContext Result.success(it) }
            }
            try {
                val bulletin = fetchOfficialBulletin()
                _cachedBulletin.value = bulletin
                Result.success(bulletin)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Preserve last verified data in memory but never report a
                // network failure or made-up data as a successful refresh.
                Result.failure(error)
            }
        }

    override suspend fun getBulletinByWeek(weekNumber: Int): Result<WeeklyBulletinResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(fetchOfficialBulletin(weekNumber))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    override suspend fun refreshLiveOdds(): Result<List<BulletinMatchDto>> =
        Result.failure(UnsupportedOperationException(
            "Resmî Spor Toto maç programı 1/0/2 oynanma yüzdelerini ve canlı oranları yayımlamıyor."
        ))

    override suspend fun getDomainMatches(forceRefresh: Boolean): Result<List<TotoMatch>> {
        return getWeeklyBulletin(forceRefresh).map { bulletin ->
            bulletin.matches.map { it.toDomainTotoMatch() }
        }
    }
}
