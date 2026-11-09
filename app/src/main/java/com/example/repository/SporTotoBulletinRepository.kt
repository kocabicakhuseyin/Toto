package com.example.repository

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
 * Repository interface for managing weekly Spor Toto bulletin, matches, dates, and live odds.
 */
interface SporTotoBulletinRepository {
    val cachedBulletin: StateFlow<WeeklyBulletinResponse?>

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
