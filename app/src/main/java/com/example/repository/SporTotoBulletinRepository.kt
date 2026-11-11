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

    override suspend fun getWeeklyBulletin(forceRefresh: Boolean): Result<WeeklyBulletinResponse> =
        withContext(Dispatchers.IO) {
            try {
                if (!forceRefresh && _cachedBulletin.value != null) {
                    return@withContext Result.success(_cachedBulletin.value!!)
                }

                val response = apiService.getCurrentWeeklyBulletin()
                if (response.isSuccessful && response.body() != null) {
                    val bulletin = response.body()!!
                    _cachedBulletin.value = bulletin
                    Result.success(bulletin)
                } else {
                    // Fallback to offline standard bulletin if server returns an error code
                    val fallback = NetworkModule.createFallbackBulletin()
                    _cachedBulletin.value = fallback
                    Result.success(fallback)
                }
            } catch (e: Exception) {
                // Return fallback on network exception so the app never fails
                val fallback = _cachedBulletin.value ?: NetworkModule.createFallbackBulletin()
                _cachedBulletin.value = fallback
                Result.success(fallback)
            }
        }

    override suspend fun getBulletinByWeek(weekNumber: Int): Result<WeeklyBulletinResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getBulletinByWeek(weekNumber)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val fallback = NetworkModule.createFallbackBulletin(weekNumber)
                    Result.success(fallback)
                }
            } catch (e: Exception) {
                Result.success(NetworkModule.createFallbackBulletin(weekNumber))
            }
        }

    override suspend fun refreshLiveOdds(): Result<List<BulletinMatchDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getLiveOdds()
                if (response.isSuccessful && response.body() != null) {
                    val updatedMatches = response.body()!!
                    // Update in-memory cached bulletin with refreshed live odds
                    _cachedBulletin.value?.let { current ->
                        _cachedBulletin.value = current.copy(matches = updatedMatches)
                    }
                    Result.success(updatedMatches)
                } else {
                    Result.success(_cachedBulletin.value?.matches ?: emptyList())
                }
            } catch (e: Exception) {
                Result.success(_cachedBulletin.value?.matches ?: emptyList())
            }
        }

    override suspend fun getDomainMatches(forceRefresh: Boolean): Result<List<TotoMatch>> {
        val result = getWeeklyBulletin(forceRefresh)
        return result.map { bulletin ->
            bulletin.matches.map { it.toDomainTotoMatch() }
        }
    }
}
