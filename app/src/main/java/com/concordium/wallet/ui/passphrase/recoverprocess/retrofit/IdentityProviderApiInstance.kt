package com.concordium.wallet.ui.passphrase.recoverprocess.retrofit

import com.concordium.wallet.data.model.RecoverErrorResponse
import com.concordium.wallet.data.model.RecoverResponse
import com.concordium.wallet.util.Log
import com.google.gson.Gson
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

        suspend fun safeRecoverCall(url: String?): Pair<Boolean, RecoverResponse?> {
            try {
                val response = api.recover(url)
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.value != null)
                            return Pair(true, it)
                        else
                            return Pair(true, null)
                    }
                } else {
                    return if (response.errorBody() != null && response.code() == 404) {
                        val errorResponse: RecoverErrorResponse = Gson().fromJson(response.errorBody()!!.charStream(), RecoverErrorResponse::class.java)
                        Log.d("${errorResponse.code} ${errorResponse.message} on $url")
                        Pair(true, null)
                    } else {
                        Pair(false, null)
                    }
                }
            } catch (t: Throwable) {
                Log.d(Log.toString(t))
            }
            return Pair(false, null)
        }
    }
}
