package com.concordium.wallet

object AppConfig {

    const val useOfflineMock = BuildConfig.USE_BACKEND_MOCK

    val proxyBaseUrl: String
        get() = BuildConfig.URL_PROXY_BASE

    val appVersion: String
        get() = if (BuildConfig.ENV_NAME != "production") {
            BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_NUMBER + ") " + (if (BuildConfig.DEBUG) " (debug)" else "")
        } else BuildConfig.VERSION_NAME

    val net: String
        get() = if (BuildConfig.ENV_NAME == "production") "Mainnet" else "Testnet"
}