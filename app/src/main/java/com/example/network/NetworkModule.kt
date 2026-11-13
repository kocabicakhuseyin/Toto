package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Spor Toto match lists come exclusively from the Teşkilat Başkanlığı API.
 * No interceptor is allowed to turn an HTTP/network failure into a fabricated
 * successful bulletin response.
 */
object NetworkModule {
    const val OFFICIAL_BULLETIN_URL = "https://webapi.sportoto.gov.tr/"

    val moshi: Moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    private val bulletinClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val apiService: SporTotoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OFFICIAL_BULLETIN_URL)
            .client(bulletinClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SporTotoApiService::class.java)
    }

    // Scores have their own client and must never be replaced by invented data.
    val liveScoreService: ApiFootballLiveService by lazy {
        Retrofit.Builder()
            .baseUrl("https://v3.football.api-sports.io/")
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiFootballLiveService::class.java)
    }

    val scoreCardsService: ScoreCardsLiveService by lazy {
        Retrofit.Builder()
            .baseUrl("https://scorecards.in/")
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ScoreCardsLiveService::class.java)
    }
}
