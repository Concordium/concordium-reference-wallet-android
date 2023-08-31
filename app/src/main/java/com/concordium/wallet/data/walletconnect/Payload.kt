package com.concordium.wallet.data.walletconnect

import java.math.BigInteger

data class Payload(
    val address: ContractAddress,
    val amount: String,
    var maxEnergy: BigInteger,
    val message: String,
    val receiveName: String
)
