package com.concordium.wallet.ui.passphrase.recoverprocess.retrofit

import com.concordium.wallet.data.model.IdentityTokenContainer
import com.concordium.wallet.data.model.RecoverResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class IdentityProviderApiInstance {
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

        private val api: IdentityProviderApi by lazy {
            retrofit.create(IdentityProviderApi::class.java)
        }

        suspend fun safeRecoverCall(url: String?): RecoverResponse? {
            try {
                val response = api.recover(url)
                if (response.isSuccessful) {
                    response.body()?.let {
                        return it
                    }
                }
            } catch (t: Throwable) {
            }
            return null
        }

        suspend fun safeIdentityCall(url: String?): IdentityTokenContainer? {
            try {
                val response = api.identity(url)
                if (response.isSuccessful) {
                    response.body()?.let {
                        return it
                    }
                }
            } catch (t: Throwable) {
            }
            return null
        }
    }
}
