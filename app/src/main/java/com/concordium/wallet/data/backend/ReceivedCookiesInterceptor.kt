package com.concordium.wallet.data.backend

import com.concordium.wallet.App
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpCookie

class ReceivedCookiesInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        response.header("Set-Cookie")?.let { cookies ->
            HttpCookie.parse(cookies)?.let { httpCookies ->
                httpCookies.firstOrNull { it.name == "concordium-wallet-proxy-session" }?.let { sessionCookie ->
                    App.appCore.sessionCookie = sessionCookie.value
                }
            }
        }
        return response
    }
}
