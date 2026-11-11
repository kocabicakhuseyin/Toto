package com.example.repository

import com.example.BuildConfig
import com.example.model.MatchLiveScore
import com.example.model.MatchStatus
import com.example.model.TotoMatch
import com.example.network.NetworkModule
import com.example.network.SporTotoApiService
import com.example.network.model.BulletinMatchDto
import com.example.network.model.WeeklyBulletinResponse
import com.example.network.model.toDomainTotoMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository interface for managing weekly Spor Toto bulletin, matches, dates, live odds, and real-time scores.
 */
interface SporTotoBulletinRepository {
    val cachedBulletin: StateFlow<WeeklyBulletinResponse?>
    val liveScores: StateFlow<List<MatchLiveScore>>

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
    private val liveScoreService: com.example.network.ApiFootballLiveService = NetworkModule.liveScoreService,
    private val liveScoreApiKey: String = BuildConfig.API_FOOTBALL_KEY
) : SporTotoBulletinRepository {

    private val _cachedBulletin = MutableStateFlow<WeeklyBulletinResponse?>(null)
    override val cachedBulletin: StateFlow<WeeklyBulletinResponse?> = _cachedBulletin.asStateFlow()

    private val _liveScores = MutableStateFlow<List<MatchLiveScore>>(emptyList())
    override val liveScores: StateFlow<List<MatchLiveScore>> = _liveScores.asStateFlow()

    override suspend fun pollLiveScores(): Result<List<MatchLiveScore>> =
        withContext(Dispatchers.IO) {
            if (liveScoreApiKey.isBlank()) {
                _liveScores.value = emptyList()
                return@withContext Result.failure(
                    IllegalStateException("Canlı skor servisi henüz etkinleştirilmedi.")
                )
            }
            try {
                val response = liveScoreService.getLiveFixtures(liveScoreApiKey)
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _liveScores.value = emptyList()
                    return@withContext Result.failure(
                        IllegalStateException("Canlı skor servisi yanıt vermedi (HTTP ${response.code()}).")
                    )
                }
                val serviceErrors = body.errors
                if ((serviceErrors is Map<*, *> && serviceErrors.isNotEmpty()) ||
                    (serviceErrors is List<*> && serviceErrors.isNotEmpty())) {
                    _liveScores.value = emptyList()
                    return@withContext Result.failure(IllegalStateException("Skor servisi isteği reddetti; API anahtarını ve kotayı kontrol edin."))
                }
                // API-FOOTBALL fixture IDs are globally unique. They are NOT
                // Sportoto coupon numbers; do not use these to calculate coupon outcomes.
                val actualScores = body.response.map { item ->
                    val status = when (item.fixture.status.short.uppercase()) {
                        "HT" -> MatchStatus.HALF_TIME
                        "FT", "AET", "PEN" -> MatchStatus.FINISHED
                        "1H", "2H", "ET", "BT", "P", "LIVE", "INT", "SUSP" -> MatchStatus.LIVE
                        else -> MatchStatus.UPCOMING
                    }
                    MatchLiveScore(
                        matchNumber = item.fixture.id,
                        homeTeam = item.teams.home.name,
                        awayTeam = item.teams.away.name,
                        homeScore = item.goals.home,
                        awayScore = item.goals.away,
                        status = status,
                        minuteText = when {
                            status == MatchStatus.HALF_TIME -> "İY"
                            item.fixture.status.elapsed != null -> "${item.fixture.status.elapsed}'"
                            else -> "—"
                        },
                        matchDate = item.fixture.date.orEmpty(),
                        league = item.league?.name.orEmpty()
                    )
                }
                _liveScores.value = actualScores
                Result.success(actualScores)
            } catch (_: Exception) {
                _liveScores.value = emptyList()
                Result.failure(IllegalStateException("Canlı skor verisi alınamadı. Ağ bağlantısını kontrol edin."))
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
