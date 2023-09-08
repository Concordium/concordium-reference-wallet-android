package com.concordium.wallet.data.walletconnect

sealed interface Payload {

    data class ContractUpdateTransaction(
        val address: ContractAddress,
        val amount: String,
        var maxEnergy: Int,
        var maxContractExecutionEnergy: Int,
        val message: String,
        val receiveName: String
    ) : Payload

    data class AccountTransaction(
        val amount: String,
        val toAddress: String
    ) : Payload
}
