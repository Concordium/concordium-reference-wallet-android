package com.concordium.wallet.data.walletconnect

sealed interface Payload {

    data class ContractUpdateTransaction(
        val address: ContractAddress,
        val amount: String,
        var maxEnergy: Long,
        var maxContractExecutionEnergy: Long,
        val message: String,
        val receiveName: String
    ) : Payload

    data class AccountTransaction(
        val amount: String,
        val toAddress: String
    ) : Payload
}
