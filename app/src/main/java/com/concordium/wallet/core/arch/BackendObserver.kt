package com.concordium.wallet.core.arch

import androidx.lifecycle.Observer
import com.concordium.wallet.core.backend.BackendResource

/**
 * This class has the responsibility to return more specific callbacks for a BackendEventResource.
 * It is used instead of an Observer when calling observe on a LiveData object.
 * @param <T>
 */

class BackendObserver<T> : Observer<BackendResource<T>> {

    override fun onChanged(backendResource: BackendResource<T>) {
        onDone()
        val exception = backendResource.exception
        if (exception != null) {
            onException(exception)
        } else {
            val data = backendResource.data
            if (data != null) {
                onSuccess(data)
            }
        }
        return
        //custom exception
        onException(Exception("No backend resource set"))
    }

    fun onDone() {

    }

    @Suppress("UNUSED_PARAMETER")
    fun onSuccess(data: T) {

    }

    @Suppress("UNUSED_PARAMETER")
    fun onError(error: Error) {

    }

    @Suppress("UNUSED_PARAMETER")
    fun onException(e: Exception) {

    }
}