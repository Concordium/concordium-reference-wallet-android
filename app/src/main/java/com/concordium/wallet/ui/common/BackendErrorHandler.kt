package com.concordium.wallet.ui.common

import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.backend.BackendErrorException
import com.concordium.wallet.core.backend.ErrorParser
import com.concordium.wallet.core.backend.TransactionSimulationException
import com.concordium.wallet.util.Log
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object BackendErrorHandler {

    fun getExceptionStringRes(e: Throwable): Int {
        when (e) {
            // Default situation when no internet connection
            is UnknownHostException -> return R.string.app_error_backend_unknown_host_exception

            is ConnectException -> return R.string.app_error_backend_connect_exception

            is SocketTimeoutException -> return R.string.app_error_backend_unknown_sockettimeout_exception

            is BackendErrorException -> return getExceptionStringRes(e)

            is TransactionSimulationException -> return R.string.app_error_backend_transaction_simulation_failed

            else -> {
                Log.e("Exception from backend communication", e)
                return R.string.app_error_backend_unknown2
            }
        }
    }

    fun getExceptionStringRes(backendErrorException: BackendErrorException): Int {
        return when (backendErrorException.error.error) {
            0 -> {
                R.string.app_error_backend_internal_server
            }

            1 -> {
                R.string.backend_error_transaction_rejected
            }

            else -> {
                Log.e(
                    "Exception from backend communication - unknown error code",
                    backendErrorException
                )
                R.string.app_error_backend_unknown_error_code
            }
        }
    }

    fun getExceptionStringResOrNull(backendError: BackendError): Int? {
        return when (backendError.error) {
            0 -> {
                R.string.app_error_backend_internal_server
            }

            else -> {
                null
            }
        }
    }

    fun getCoroutineBackendException(e: Exception): Exception? {
        if (e is CancellationException) {
            // When the coroutines are cancelled, there should not be shown an error
            return null
        }
        var ex = e
        if (e is HttpException) {
            val response = e.response()
            if (response != null) {
                val error = ErrorParser.parseError(response)
                if (error != null) {
                    ex = BackendErrorException(error)
                }
            }
        }
        return ex
    }

    fun getCoroutineExceptionStringRes(e: Exception): Int? {
        val ex = getCoroutineBackendException(e)
        if (ex != null) {
            return getExceptionStringRes(ex)
        }
        return null
    }
}


