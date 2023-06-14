package com.concordium.wallet.util

object EncodingUtil {

    fun toHexString(input: ByteArray): String {
        return input.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
    }
}