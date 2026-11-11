package com.example.network

import com.example.model.PoolCouponDto
import com.example.model.PoolCouponUpload
import com.example.model.PoolColumnRank
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class PoolAuthCredentials(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class PoolRefreshRequest(@Json(name = "refresh_token") val refreshToken: String)

@JsonClass(generateAdapter = true)
data class PoolAuthUser(val id: String = "")

@JsonClass(generateAdapter = true)
data class PoolAuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    val user: PoolAuthUser? = null
)

@JsonClass(generateAdapter = true)
data class PoolRankRequest(
    @Json(name = "p_week_key") val weekKey: String,
    @Json(name = "p_match_labels") val matchLabels: List<String>,
    @Json(name = "p_final") val finalCodes: String,
    @Json(name = "p_live") val liveCodes: String,
    @Json(name = "p_limit") val limit: Int = 40
)

@JsonClass(generateAdapter = true)
data class PoolRankResponse(
    @Json(name = "coupon_id") val couponId: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "column_number") val columnNumber: Long,
    val predictions: String,
    @Json(name = "finished_correct") val finishedCorrect: Int,
    @Json(name = "finished_wrong") val finishedWrong: Int,
    @Json(name = "live_correct") val liveCorrect: Int,
    @Json(name = "live_wrong") val liveWrong: Int,
    @Json(name = "maximum_possible") val maximumPossible: Int,
    @Json(name = "available_results") val availableResults: Int
) {
    fun asDomain(): PoolColumnRank = PoolColumnRank(
        couponId, displayName, columnNumber.toInt(), predictions,
        finishedCorrect, finishedWrong, liveCorrect, liveWrong,
        maximumPossible, availableResults
    )
}

/** Supabase Auth and PostgREST; only the publishable key is included in the app. */
interface PoolApiService {
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") publishableKey: String,
        @Body body: PoolAuthCredentials
    ): Response<PoolAuthResponse>

    @POST("auth/v1/token")
    suspend fun signIn(
        @Header("apikey") publishableKey: String,
        @Query("grant_type") grantType: String = "password",
        @Body body: PoolAuthCredentials
    ): Response<PoolAuthResponse>

    @POST("auth/v1/token")
    suspend fun refresh(
        @Header("apikey") publishableKey: String,
        @Query("grant_type") grantType: String = "refresh_token",
        @Body body: PoolRefreshRequest
    ): Response<PoolAuthResponse>

    @GET("rest/v1/pool_coupons")
    suspend fun listShared(
        @Header("apikey") publishableKey: String,
        @Header("Authorization") bearer: String,
        @Query("week_key") weekFilter: String,
        @Query("select") select: String = "id,owner_id,week_key,coupon_title,masked_name,match_labels,columns,shared_at",
        @Query("order") order: String = "shared_at.desc",
        @Query("limit") limit: Int = 100
    ): Response<List<PoolCouponDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/pool_coupons")
    suspend fun publish(
        @Header("apikey") publishableKey: String,
        @Header("Authorization") bearer: String,
        @Body coupon: PoolCouponUpload
    ): Response<List<PoolCouponDto>>

    @DELETE("rest/v1/pool_coupons")
    suspend fun withdraw(
        @Header("apikey") publishableKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    @POST("rest/v1/rpc/pool_rank_for_fixture_set")
    suspend fun rank(
        @Header("apikey") publishableKey: String,
        @Header("Authorization") bearer: String,
        @Body input: PoolRankRequest
    ): Response<List<PoolRankResponse>>
}
