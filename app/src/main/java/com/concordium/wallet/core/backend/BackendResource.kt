package com.concordium.wallet.core.backend

/**
 * This class has the responsibility to hold data for some kind of resource that is either available as the type T or not available because of an exception.
 * Or it can be available as an Error type.
 * Eg. the resource could be a Response type used in a Retrofit call, where there will sometimes be thrown an exception, and the response will not be available.
 * @param <T>
 */

open class BackendResource<T> {
    var data: T? = null
    var exception: Exception? = null
    var error: BackendError? = null
}
