package com.concordium.wallet.util

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.roundToDecimalPlace(numberOfDecimalPlaces: Int) =
    BigDecimal(this).setScale(numberOfDecimalPlaces, RoundingMode.DOWN).toDouble()