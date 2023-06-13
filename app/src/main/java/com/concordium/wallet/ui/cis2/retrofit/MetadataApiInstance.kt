package com.concordium.wallet.ui.cis2.retrofit

import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.util.Log
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.backend.BackendErrorException
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import java.security.MessageDigest
import java.util.Base64
import java.io.IOException

fun sha256(input: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest(input)
    return Base64.getEncoder().encodeToString(bytes)
}

class IncorrectChecksumException(): IOException("Body does not have specified checksum")

class MetadataApiInstance {
    companion object {
        private val retrofit by lazy {
            val client = OkHttpClient.Builder()
                .addInterceptor({ chain ->
                                    val request = chain.request();
                                    val checksum = request.url.queryParameter("metadataChecksum")
                                    val url = request.url.newBuilder().removeAllQueryParameters("metadataChecksum").build();
                                    val response = chain.proceed(request.newBuilder().url(url).build())
                                    if (checksum != null) {
                                        val rawBody = response.body
                                        if (rawBody != null) {
                                            val hash = sha256(rawBody.bytes())
                                            if (hash != checksum) {
                                                throw IncorrectChecksumException()
                                            }
                                        }
                                    }
                                    response
                                })
                .build()
            Retrofit.Builder()
                .baseUrl("https://some.api.url/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        private val api: MetadataApi by lazy {
            retrofit.create(MetadataApi::class.java)
        }

        suspend fun safeMetadataCall(url: String?, checksum: String?): Result<TokenMetadata> {
            try {
                val response = api.metadata(url, checksum)
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
