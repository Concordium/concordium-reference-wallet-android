package com.concordium.wallet.core.backend

/**
 * This class has the responsibility to hold data as the superclass describes, and besides that it represents the resource as an event.
 * It keeps track of if the event has been handled.
 * @param <T>
 */
class BackendEventResource<T> : BackendResource<T>() {

    private var hasBeenHandled = false

    /**
     * Returns the data if event has not been handled and should be handled.
     * This is only useful if we do not need the error or the exception.
     * @return
     */
    val contentIfNotHandled: T?
        get() {
            if (hasBeenHandled) {
                return null
            } else {
                hasBeenHandled = true
                return data
            }
        }

    /**
     * Returns true if event has not been handled and should be handled
     * @return
     */
    fun handleIfNotHandled(): Boolean {
        if (hasBeenHandled) {
            return false
        } else {
            hasBeenHandled = true
            return true
        }
    }

}