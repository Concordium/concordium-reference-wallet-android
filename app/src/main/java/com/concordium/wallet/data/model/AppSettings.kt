package com.concordium.wallet.data.model

data class AppSettings(
    val status: String,
    val url: String?
) {
    companion object {
        const val APP_VERSION_STATUS_OK = "ok"
        const val APP_VERSION_STATUS_WARNING = "warning"
        const val APP_VERSION_STATUS_NEEDS_UPDATE = "needsUpdate"
    }
}
