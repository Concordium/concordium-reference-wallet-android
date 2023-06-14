package com.concordium.wallet.ui.cis2.retrofit

import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.HashUtil
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.backend.BackendErrorException
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import java.io.IOException

class IncorrectChecksumException(): IOException("Body does not have specified checksum")

internal const val METADATA_CHECKSUM_FAKE_PARAMETER = "INTERNAL_metadataChecksum"

class MetadataApiInstance {
    companion object {
        private val retrofit by lazy {
            val client = OkHttpClient.Builder()
                .addInterceptor({ chain ->
                                    val request = chain.request();
                                    // Extract the checksum parameter that we added to the url, so that we can verify the response body matches
                                    // The parameter is added to the url in safeMetadataCall, so that we can access it in this interceptor, before the body is consumed by the parser.
                                    val checksum = request.url.queryParameter(METADATA_CHECKSUM_FAKE_PARAMETER)
                                    // Rebuild the url without the checksum parameter
                                    val url = request.url.newBuilder().removeAllQueryParameters(METADATA_CHECKSUM_FAKE_PARAMETER).build();
                                    val response = chain.proceed(request.newBuilder().url(url).build())
                                    // If a checksum was supplied, then we verify that the body matches
                                    if (checksum != null) {
                                        val rawBody = response.body
                                        if (rawBody != null) {
                                            val hash = HashUtil.sha256(rawBody.bytes())
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
