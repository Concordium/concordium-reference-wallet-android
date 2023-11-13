package com.concordium.wallet.util

import kotlin.math.floor
import kotlin.math.pow

fun Double.truncateDecimals(decimalPoint: Int): Double =
    floor(this * 10.0.pow(decimalPoint)) / 10.0.pow(decimalPoint)