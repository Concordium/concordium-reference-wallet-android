package com.concordium.wallet.core.arch

import androidx.lifecycle.Observer
import com.concordium.wallet.core.backend.BackendEventResource

/**
 * This class has the responsibility to return more specific callbacks for a BackendEventResource.
 * It is used instead of an Observer when calling observe on a LiveData object.
 * As it operates on an event resource it only results in a callback, if the event has not already been handled.
 * @param <T>
 */

open class BackendEventObserver<T> : Observer<BackendEventResource<T>> {

    override fun onChanged(backendResource: BackendEventResource<T>) {
        onDone()
        if (backendResource.handleIfNotHandled()) {
            val exception = backendResource.exception
            if (exception != null) {
                onException(exception)
            } else {
                val data = backendResource.data
                if (data != null) {
                    onSuccess(data)
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    open fun onDone() {

    }

    @Suppress("UNUSED_PARAMETER")
    open fun onSuccess(data: T) {

    }

    @Suppress("UNUSED_PARAMETER")
    open fun onException(e: Exception) {

    }

}