package com.concordium.wallet.core.arch

import androidx.lifecycle.Observer

/**
 * This class has the responsibility to only call a callback when the event has not already been handled.
 * It is used instead of an Observer when calling observe on a LiveData object.
 * @param <T>
 */

open class EventObserver<T> : Observer<Event<T>> {

    override fun onChanged(event: Event<T>) {
        if (event != null) {
            val value = event.contentIfNotHandled
            if (value != null) {
                onUnhandledEvent(value)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    open fun onUnhandledEvent(value: T) {

    }
}