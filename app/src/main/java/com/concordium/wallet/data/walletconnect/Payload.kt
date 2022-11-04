package com.concordium.wallet.data.walletconnect

data class Payload(
    val address: ContractAddress,
    val amount: String,
    val maxContractExecutionEnergy: Int,
    val message: String,
    val receiveName: String
)
