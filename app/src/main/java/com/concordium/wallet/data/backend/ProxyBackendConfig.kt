package com.concordium.wallet.data.backend

import com.concordium.wallet.AppConfig
import com.concordium.wallet.BuildConfig
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ProxyBackendConfig(val gson: Gson) {

    val retrofit: Retrofit
    val backend: ProxyBackend

    init {
        retrofit = initializeRetrofit(AppConfig.proxyBaseUrl)
        backend = retrofit.create(ProxyBackend::class.java)
    }

    private fun initializeRetrofit(baseUrl: String): Retrofit {

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(initializeOkkHttp())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        return retrofit
    }

    private fun initializeOkkHttp(): OkHttpClient {

        var okHttpClientBuilder = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cache(null)
            .addInterceptor(ModifyHeaderInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level =
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            })

        if (AppConfig.useOfflineMock) {
            okHttpClientBuilder = okHttpClientBuilder.addInterceptor(OfflineMockInterceptor())
        }
        val okHttpClient = okHttpClientBuilder.build()
        return okHttpClient
    }
}