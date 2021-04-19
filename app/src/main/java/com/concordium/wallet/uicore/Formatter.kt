package com.concordium.wallet.uicore

object Formatter {

    fun formatAsFirstEight(text: String): String {
        if (text.length < 8) {
            return text
        }
        return text.substring(0, 8)
    }
}