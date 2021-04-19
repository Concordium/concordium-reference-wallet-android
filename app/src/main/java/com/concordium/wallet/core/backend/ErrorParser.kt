package com.concordium.wallet.core.backend

import com.concordium.wallet.App
import retrofit2.Response
import java.io.IOException

object ErrorParser {

    fun parseError(response: Response<*>): BackendError? {
        val responseErrorBody = response.errorBody() ?: return BackendError(
            response.code(),
            "No Error object could be parsed"
        )

        val retrofit = App.appCore.proxybackendConfig.retrofit
        val converter =
            retrofit.responseBodyConverter<BackendError>(
                BackendError::class.java,
                arrayOfNulls<Annotation>(0)
            )

        try {
            return converter.convert(responseErrorBody)
        } catch (e: IOException) {
        }
        return BackendError(
            response.code(),
            "No Error object could be parsed"
        )
    }
}