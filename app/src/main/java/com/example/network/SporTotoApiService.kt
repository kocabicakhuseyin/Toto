package com.example.network

import com.example.network.model.BulletinMatchDto
import com.example.network.model.WeeklyBulletinResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service definition for fetching weekly Spor Toto bulletin and live odds from an external sports API.
 */
interface SporTotoApiService {

    /**
     * Fetches the current active weekly Spor Toto bulletin with 15 matches and odds.
     */
    @GET("api/v1/sportoto/bulletin/current")
    suspend fun getCurrentWeeklyBulletin(): Response<WeeklyBulletinResponse>

    /**
     * Fetches a specific weekly Spor Toto bulletin by week number (e.g. week 38).
     */
    @GET("api/v1/sportoto/bulletin/week/{weekNumber}")
    suspend fun getBulletinByWeek(
        @Path("weekNumber") weekNumber: Int
    ): Response<WeeklyBulletinResponse>

    /**
     * Fetches updated real-time live betting odds for current bulletin matches.
     */
    @GET("api/v1/sportoto/odds/live")
    suspend fun getLiveOdds(
        @Query("week") weekNumber: Int? = null
    ): Response<List<BulletinMatchDto>>
}
