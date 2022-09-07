package com.concordium.wallet.util

fun ByteArray.toHex() = joinToString("") {
    Integer.toUnsignedString(java.lang.Byte.toUnsignedInt(it), 16).padStart(2, '0')
}

fun <T> MutableMap<T, Int>.increase(key: T, more: Int = 1) = merge(key, more, Int::plus)
