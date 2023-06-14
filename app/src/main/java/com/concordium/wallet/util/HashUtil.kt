package com.concordium.wallet.util

import java.security.MessageDigest

object HashUtil {

    fun sha256(input: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input)
        return bytes.toHex()
    }

}
