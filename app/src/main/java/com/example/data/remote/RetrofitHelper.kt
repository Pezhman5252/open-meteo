package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitHelper {
    private const val DIRECT_OPEN_METEO_URL = "https://api.open-meteo.com/"
    private val BASE_URL: String = getBaseUrl()

    private fun getBaseUrl(): String {
        val configUrl = try {
            BuildConfig.OPEN_METEO_BASE_URL
        } catch (e: Throwable) {
            null
        }
        val finalUrl = if (configUrl.isNullOrBlank() || configUrl == "OPEN_METEO_BASE_URL" || configUrl.contains("MY_")) {
            "https://mountain-weather-api.persianboy-1991g.workers.dev/"
        } else {
            if (configUrl.endsWith("/")) configUrl else "$configUrl/"
        }
        Log.d("RetrofitHelper", "Configured OpenMeteo Base URL: $finalUrl")
        return finalUrl
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("User-Agent", "Mountain-Weather-Secure-Proxy/1.0")
                .header("Accept", "application/json")
                .build()
            chain.proceed(newRequest)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val apiService: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApiService::class.java)
    }

    val directApiService: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DIRECT_OPEN_METEO_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApiService::class.java)
    }
}

