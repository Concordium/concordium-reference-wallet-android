package com.concordium.wallet.data.backend

import okhttp3.Interceptor
import okhttp3.Response
import java.util.*

class ModifyHeaderInterceptor : Interceptor {

    companion object {
        const val AcceptLanguage = "Accept-Language"
    }

    override fun intercept(chain: Interceptor.Chain): Response? {
        val language = Locale.getDefault().language
        val originalRequest = chain.request()
        val requestWithHeaders = originalRequest.newBuilder()
            .header(AcceptLanguage, language)
            .build()
        return chain.proceed(requestWithHeaders)
    }
}