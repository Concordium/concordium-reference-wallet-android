package com.concordium.wallet.data.backend

import com.concordium.wallet.App
import okhttp3.Interceptor
import okhttp3.Response

class ReceivedCookiesInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        App.appCore.setCookie = response.header("Set-Cookie")
        return response
    }
}
