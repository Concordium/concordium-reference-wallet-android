package com.concordium.wallet.data.model

import java.math.BigInteger

data class TransferCost(
    val energy: Long,
    val cost: BigInteger,
    val success: Boolean?
){
    constructor(
        energy: Long,
        euroPerEnergy: SimpleFraction,
        microGTUPerEuro: SimpleFraction,
    ) : this(
        energy = energy,
        cost = (energy.toBigInteger() * euroPerEnergy.numerator * microGTUPerEuro.numerator)
            .divide(euroPerEnergy.denominator * microGTUPerEuro.denominator),
        success = null,
    )
}
