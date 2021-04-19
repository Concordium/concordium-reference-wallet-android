package com.concordium.wallet.core.backend

import retrofit2.Call

class BackendRequest<T>(
    var call: Call<T>,
    var success: ((T) -> Unit)?,
    var failure: ((Throwable) -> Unit)? = null
) {

    fun dispose() {
        call.cancel()
        success = null
        failure = null
    }
}
