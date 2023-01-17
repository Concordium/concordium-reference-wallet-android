package com.concordium.wallet.data.walletconnect

data class Payload(
    val address: ContractAddress,
    val amount: String,
    var maxEnergy: Long,
    var maxContractExecutionEnergy: Int,
    val message: String,
    val receiveName: String
)
