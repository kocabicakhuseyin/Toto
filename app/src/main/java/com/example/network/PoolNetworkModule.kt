package com.example.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/** The publishable key is intentionally public; RLS protects member data. */
object PoolNetworkModule {
    private const val BASE_URL = "https://fmmszozzbylbqulhfrln.supabase.co/"
    const val PUBLISHABLE_KEY = "sb_publishable_LIeFCfNieoM_Hq69fcfM5w_MsrBtUB9"

    val service: PoolApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    // Auth tokens and emails must never be logged.
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(NetworkModule.moshi))
            .build()
            .create(PoolApiService::class.java)
    }
}
