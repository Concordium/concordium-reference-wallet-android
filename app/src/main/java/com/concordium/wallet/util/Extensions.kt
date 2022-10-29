package com.concordium.wallet.util

import android.content.Intent
import android.os.Build
import java.io.Serializable

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
