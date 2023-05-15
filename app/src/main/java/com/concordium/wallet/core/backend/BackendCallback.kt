package com.concordium.wallet.core.backend

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class BackendCallback<T> : Callback<T> {

    override fun onResponse(call: Call<T>, response: Response<T>) {

        if (isValidResponse(response)) {
            val body = response.body()
            if (body != null) {
                onResponseData(body)
            }
        } else {
            // Parse errorBody to error object
            try {
                val error = ErrorParser.parseError(response)
                if(error != null) {
                    onFailure(BackendErrorException(error))
                    return
                }
            } catch (e: Exception) {
                onFailure(Exception("Response is not valid - error parsing failed", e))
                return
            }
            onFailure(Exception("Response is not valid - error parsing failed"))
        }
    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        onFailure(t)
    }

    protected abstract fun onResponseData(response: T)

    protected abstract fun onFailure(t: Throwable)

    private fun isValidResponse(response: Response<T>): Boolean {
        val responseBody = response.body()
        if (responseBody != null && response.isSuccessful) {
            return true
        }
        return false
    }

}