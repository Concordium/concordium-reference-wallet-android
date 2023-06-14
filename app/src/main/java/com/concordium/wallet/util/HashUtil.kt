package com.concordium.wallet.util

import java.security.MessageDigest
import java.util.Base64

object HashUtil {

    fun sha256(input: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input)
        return Base64.getEncoder().encodeToString(bytes)
    }

}
