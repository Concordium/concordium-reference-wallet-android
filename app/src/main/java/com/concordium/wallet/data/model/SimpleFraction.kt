package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger

/**
 * A fraction defined as 2 integers:
 * ```
 *  N
 *  –
 *  D
 * ```
 */
class SimpleFraction(
    val numerator: BigInteger,
    val denominator: BigInteger,
): Serializable
