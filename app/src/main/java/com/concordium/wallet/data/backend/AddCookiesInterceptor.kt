package com.concordium.wallet.data.backend

import com.concordium.wallet.App
import okhttp3.Interceptor
import okhttp3.Response

class AddCookiesInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        App.appCore.sessionCookie?.let { sessionCookie ->
            builder.addHeader("Cookie", sessionCookie)
        }
        return chain.proceed(builder.build())
    }
}
