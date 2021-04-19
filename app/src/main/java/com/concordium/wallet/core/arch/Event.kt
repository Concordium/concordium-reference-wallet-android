package com.concordium.wallet.core.arch

class Event<T>(private val content: T) {
    private var hasBeenHandled = false

    /**
     *
     * @return
     */
    val contentIfNotHandled: T?
        get() {
            if (hasBeenHandled) {
                return null
            } else {
                hasBeenHandled = true
                return content
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
