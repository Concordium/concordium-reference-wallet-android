package com.concordium.wallet.util

import android.content.Intent
import android.os.Build
import android.os.Bundle
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode

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
 * Allows to safely parse BigDecimal from String with default value (0 by default)
 * @return parsed value or [defaultValue] if it can't be parsed
 */
fun String?.toBigDecimal(defaultValue: BigDecimal = BigDecimal.ZERO): BigDecimal =
    try {
        if (this.isNullOrBlank())
            defaultValue
        else
            BigDecimal(this)
    } catch (e: NumberFormatException) {
        defaultValue
    }

/**
 * Scales the value rounding it down, which is a must-do for balances, amounts, etc.
 *
 * @return a [BigDecimal] whose scale is the specified [scale]
 */
fun BigDecimal.scaleAmount(scale: Int): BigDecimal =
    setScale(scale, RoundingMode.DOWN)
        .stripTrailingZerosFixed()

/**
 * @return a result of [BigDecimal.stripTrailingZeros], but with the zero value Java 7 issue fix.
 *
 * @see <a href="https://hg.openjdk.org/jdk8/jdk8/jdk/rev/2ee772cda1d6">The issue</a>
 */
fun BigDecimal.stripTrailingZerosFixed(): BigDecimal {
    return if (signum() == 0) {
        BigDecimal.ZERO
    } else {
        stripTrailingZeros()
    }
}

/**
 * @return a result o [BigDecimal.toPlainString], but with stripped trailing zeros.
 */
fun BigDecimal.toPlainStringStripped() =
    stripTrailingZerosFixed().toPlainString()

/**
 * @return true if two numbers are equal ignoring trailing zeros:
 *
 * 3.14 == 3.1400
 *
 * null == null
 */
fun BigDecimal?.equalsArithmetically(other: BigDecimal?): Boolean {
    return this == other || this != null && other != null && this.compareTo(other) == 0
}