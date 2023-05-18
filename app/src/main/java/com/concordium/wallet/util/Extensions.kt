package com.concordium.wallet.util

import android.content.Intent
import android.os.Build
import android.os.Bundle
import java.io.Serializable
import java.math.BigInteger

fun ByteArray.toHex() = joinToString("") {
    Integer.toUnsignedString(java.lang.Byte.toUnsignedInt(it), 16).padStart(2, '0')
}

fun String.toHex() = this.toByteArray().joinToString("") {
    Integer.toUnsignedString(java.lang.Byte.toUnsignedInt(it), 16).padStart(2, '0')
}

fun Float.roundUpToInt(): Int {
    return (this + 0.99).toInt()
}

fun <T : Serializable?> Intent.getSerializable(key: String, m_class: Class<T>): T {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        this.getSerializableExtra(key, m_class)!!
    else {
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        this.getSerializableExtra(key) as T
    }
}

fun <T : Serializable?> Bundle.getSerializableFromBundle(key: String, m_class: Class<T>): T {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        this.getSerializable(key, m_class)!!
    else {
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        this.getSerializable(key) as T
    }
}

/**
 * Allows to safely parse [BigInteger] from [String] with default value (0 by default)
 * @return parsed value or [defaultValue] if it can't be parsed
 */
fun String?.toBigInteger(defaultValue: BigInteger = BigInteger.ZERO): BigInteger =
    try {
        if (this.isNullOrBlank())
            defaultValue
        else
            BigInteger(this)
    } catch (e: NumberFormatException) {
        defaultValue
    }
