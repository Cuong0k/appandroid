package com.vpnapp.api

import com.vpnapp.App
import com.vpnapp.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile private var retrofit: Retrofit? = null
    @Volatile private var service: ApiService? = null

    private val authInterceptor = Interceptor { chain ->
        val authData = runBlocking {
            App.instance.preferencesManager.authData.first()
        }
        val req = chain.request().newBuilder().apply {
            if (!authData.isNullOrEmpty()) addHeader("Authorization", authData)
        }.build()
        chain.proceed(req)
    }

    private fun buildClient() = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun resolveBaseUrl(): String {
        val saved = runBlocking { App.instance.preferencesManager.baseUrl.first() }
        val url = if (!saved.isNullOrBlank()) saved else Constants.DEFAULT_BASE_URL
        return if (url.endsWith("/")) url else "$url/"
    }

    private fun getRetrofit(): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: Retrofit.Builder()
                .baseUrl(resolveBaseUrl())
                .client(buildClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .also { retrofit = it }
        }
    }

    val apiService: ApiService
        get() = service ?: synchronized(this) {
            service ?: getRetrofit().create(ApiService::class.java).also { service = it }
        }

    fun reset() {
        retrofit = null
        service = null
    }
}
