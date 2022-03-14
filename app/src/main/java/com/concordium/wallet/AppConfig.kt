package com.concordium.wallet

object AppConfig {

    const val useOfflineMock = BuildConfig.USE_BACKEND_MOCK

    val proxyBaseUrl: String
        get() = BuildConfig.URL_PROXY_BASE

    val appVersion: String
        get() =
            BuildConfig.VERSION_NAME  + " " + BuildConfig.VERSION_POSTFIX + " "+ (if (BuildConfig.DEBUG) " (debug)" else "")

}