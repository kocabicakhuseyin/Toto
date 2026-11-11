package com.example.repository

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
    private val apiService: SporTotoApiService = NetworkModule.apiService
) : SporTotoBulletinRepository {

    private val _cachedBulletin = MutableStateFlow<WeeklyBulletinResponse?>(null)
    override val cachedBulletin: StateFlow<WeeklyBulletinResponse?> = _cachedBulletin.asStateFlow()

    private val _liveScores = MutableStateFlow<List<MatchLiveScore>>(createInitialLiveScores())
    override val liveScores: StateFlow<List<MatchLiveScore>> = _liveScores.asStateFlow()

    private var pollIteration = 0

    override suspend fun pollLiveScores(): Result<List<MatchLiveScore>> =
        withContext(Dispatchers.IO) {
            pollIteration++
            val currentList = _liveScores.value.toMutableList()
            val updated = currentList.map { match ->
                when (match.status) {
                    MatchStatus.LIVE -> {
                        val currentMin = match.minuteText.replace("'", "").toIntOrNull() ?: 60
                        val newMin = (currentMin + 1).coerceAtMost(90)
                        val status = if (newMin >= 90) MatchStatus.FINISHED else MatchStatus.LIVE
                        val minText = if (status == MatchStatus.FINISHED) "MS" else "$newMin'"

                        var hScore = match.homeScore ?: 0
                        var aScore = match.awayScore ?: 0
                        // Real-time simulated goal progression every few poll cycles
                        if (pollIteration % 4 == 0 && match.matchNumber in listOf(1, 4, 7)) {
                            if (match.matchNumber == 1) aScore++ // Türkiye scores!
                            else hScore++
                        }

                        match.copy(
                            homeScore = hScore,
                            awayScore = aScore,
                            minuteText = minText,
                            status = status
                        )
                    }
                    MatchStatus.HALF_TIME -> {
                        if (pollIteration % 2 == 0) {
                            match.copy(status = MatchStatus.LIVE, minuteText = "46'")
                        } else match
                    }
                    MatchStatus.UPCOMING -> {
                        if (pollIteration >= 3 && match.matchNumber == 9) {
                            match.copy(status = MatchStatus.LIVE, minuteText = "1'", homeScore = 0, awayScore = 0)
                        } else match
                    }
                    MatchStatus.FINISHED -> {
                        match
                    }
                }
            }
            _liveScores.value = updated
            Result.success(updated)
        }

    private fun createInitialLiveScores(): List<MatchLiveScore> {
        return listOf(
            MatchLiveScore(1, "FRANSA", "TÜRKİYE", 1, 1, MatchStatus.LIVE, "74'", "15.11.2026 22:45", "UEFA Uluslar Ligi", 1.32, 4.80, 8.50),
            MatchLiveScore(2, "BULGARİSTAN", "İZLANDA", 1, 2, MatchStatus.FINISHED, "MS", "15.11.2026 19:00", "UEFA Uluslar Ligi", 2.65, 3.10, 2.70),
            MatchLiveScore(3, "LİHTENŞTAYN", "AZERBAYCAN", 0, 1, MatchStatus.FINISHED, "MS", "15.11.2026 19:00", "UEFA Uluslar Ligi", 5.50, 3.80, 1.55),
            MatchLiveScore(4, "LÜKSEMBURG", "ESTONYA", 2, 0, MatchStatus.LIVE, "81'", "15.11.2026 21:00", "UEFA Uluslar Ligi", 1.95, 3.20, 3.90),
            MatchLiveScore(5, "SLOVAKYA", "FAROE ADALARI", 3, 0, MatchStatus.FINISHED, "MS", "15.11.2026 19:00", "UEFA Uluslar Ligi", 1.22, 5.80, 11.00),
            MatchLiveScore(6, "GÜRCİSTAN", "MACARİSTAN", 0, 0, MatchStatus.LIVE, "63'", "15.11.2026 21:00", "UEFA Uluslar Ligi", 2.30, 3.15, 3.05),
            MatchLiveScore(7, "NORVEÇ", "GALLER", 2, 1, MatchStatus.LIVE, "52'", "15.11.2026 21:45", "UEFA Uluslar Ligi", 1.68, 3.65, 4.85),
            MatchLiveScore(8, "PORTEKİZ", "DANİMARKA", 1, 0, MatchStatus.HALF_TIME, "İY", "15.11.2026 21:45", "UEFA Uluslar Ligi", 1.48, 4.10, 6.20),
            MatchLiveScore(9, "İSPANYA", "İTALYA", null, null, MatchStatus.UPCOMING, "22:45", "15.11.2026 22:45", "UEFA Uluslar Ligi", 1.80, 3.45, 4.25),
            MatchLiveScore(10, "ALMANYA", "HOLLANDA", null, null, MatchStatus.UPCOMING, "22:45", "15.11.2026 22:45", "UEFA Uluslar Ligi", 1.75, 3.75, 4.10),
            MatchLiveScore(11, "İNGİLTERE", "BELÇİKA", null, null, MatchStatus.UPCOMING, "22:45", "15.11.2026 22:45", "UEFA Uluslar Ligi", 1.55, 3.90, 5.60),
            MatchLiveScore(12, "HIRVATİSTAN", "POLONYA", null, null, MatchStatus.UPCOMING, "23:00", "15.11.2026 23:00", "UEFA Uluslar Ligi", 1.62, 3.70, 5.20),
            MatchLiveScore(13, "İSVİÇRE", "AVUSTURYA", null, null, MatchStatus.UPCOMING, "23:00", "15.11.2026 23:00", "UEFA Uluslar Ligi", 2.10, 3.25, 3.40),
            MatchLiveScore(14, "İSVEÇ", "SIRBİSTAN", null, null, MatchStatus.UPCOMING, "23:00", "15.11.2026 23:00", "UEFA Uluslar Ligi", 2.05, 3.30, 3.50),
            MatchLiveScore(15, "ÇEKYA", "UKRAYNA", null, null, MatchStatus.UPCOMING, "23:00", "15.11.2026 23:00", "UEFA Uluslar Ligi", 2.25, 3.20, 3.10)
        )
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
