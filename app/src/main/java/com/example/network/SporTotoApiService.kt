package com.example.network

import com.example.network.model.OfficialGameMatchResponse
import com.example.network.model.OfficialGameRoundsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** Public program API used by the official Spor Toto Teşkilat website. */
interface SporTotoApiService {
    /** Published programs within a season, e.g. year=2026/2027. */
    @GET("api/GameRound")
    suspend fun getPublishedRounds(
        @Query("year") season: String,
        @Query("isPublished") published: Boolean = true
    ): Response<OfficialGameRoundsResponse>

    /** Match order is the official coupon's 1–15 order. */
    @GET("api/GameMatch/GetGameMatches/")
    suspend fun getRoundMatches(
        @Query("gameRoundId") roundId: Int
    ): Response<OfficialGameMatchResponse>
}
