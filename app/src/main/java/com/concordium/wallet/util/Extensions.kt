package com.concordium.wallet.util

fun ByteArray.toHex() = joinToString("") {
    Integer.toUnsignedString(java.lang.Byte.toUnsignedInt(it), 16).padStart(2, '0')
}

fun Float.roundUpToInt(): Int {
    return (this + 0.99).toInt()
}
