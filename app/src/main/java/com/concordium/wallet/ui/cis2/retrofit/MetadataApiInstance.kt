package com.concordium.wallet.ui.cis2.retrofit

import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.util.Log
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.backend.BackendErrorException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MetadataApiInstance {
    companion object {
        private val retrofit by lazy {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
            Retrofit.Builder()
                .baseUrl("https://some.api.url/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }

        private val api: MetadataApi by lazy {
            retrofit.create(MetadataApi::class.java)
        }

        suspend fun safeMetadataCall(url: String?): Result<TokenMetadata> {
            try {
                val response = api.metadata(url)
                if (response.isSuccessful) {
                    response.body()?.let {
                        return Result.success(it)
                    }
                }
                return Result.failure(BackendErrorException(BackendError(response.code(), response.errorBody().toString())))
            } catch (t: Throwable) {
                Log.d(Log.toString(t))
                return Result.failure(t)
            }
        }
    }
}
