package com.concordium.wallet.data.util

import com.concordium.wallet.data.model.Token

interface CurrencyUtil {
    fun formatGTU(value: String, withGStroke: Boolean = false, decimals: Int = 6): String
    fun formatGTU(value: Long, token: Token?): String
    fun formatGTU(value: Long, withGStroke: Boolean = false, decimals: Int = 6): String
    fun getWholePart(stringValue: String): Long?
    fun toGTUValue(stringValue: String, token: Token?): Long?
    fun toGTUValue(stringValue: String, decimals: Int = 6): Long?
    fun getGstroke(): String
}
