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

    override fun onChanged(value: BackendEventResource<T>) {
        onDone()
        if (value != null) {
            if (value.handleIfNotHandled()) {
                val exception = value.exception
                if (exception != null) {
                    onException(exception)
                } else {
                    val data = value.data
                    if (data != null) {
                        onSuccess(data)
                    }
                }
            }
            return
        }
        //custom exception
        onException(Exception("No backend resource set"))
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