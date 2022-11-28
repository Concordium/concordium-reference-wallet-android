package com.concordium.wallet.data.walletconnect

data class Payload(
    val address: ContractAddress,
    val amount: String,
    val maxEnergy: Int,
    val message: String,
    val receiveName: String
)
