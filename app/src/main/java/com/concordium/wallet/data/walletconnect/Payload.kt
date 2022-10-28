package com.concordium.wallet.data.walletconnect

data class Payload(
    val amount: Amount,
    val contractAddress: ContractAddress,
    val maxContractExecutionEnergy: String,
    val parameter: Parameter,
    val receiveName: String
)