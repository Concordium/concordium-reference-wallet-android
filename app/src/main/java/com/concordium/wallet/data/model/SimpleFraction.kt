package com.concordium.wallet.data.model

import java.math.BigInteger

/**
 * A fraction defined as 2 integers:
 * ```
 *  N
 *  –
 *  D
 * ```
 */
data class SimpleFraction(
    val numerator: BigInteger,
    val denominator: BigInteger,
)
