package com.concordium.wallet.data.backend

import com.concordium.wallet.App
import okhttp3.Interceptor
import okhttp3.Response

class AddCookiesInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        App.appCore.setCookie?.let { cookie ->
            builder.addHeader("Set-Cookie", cookie)
        }
        return chain.proceed(builder.build())
    }
}
