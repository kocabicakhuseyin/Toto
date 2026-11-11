package com.example.network

import com.example.model.PoolCouponDto
import com.example.model.PoolCouponUpload
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
        @Query("select") select: String = "id,week_key,coupon_title,masked_name,match_labels,columns,shared_at",
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
}
