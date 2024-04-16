package com.concordium.wallet.data.walletconnect

sealed interface Payload {
    data class ContractUpdateTransaction(
        val address: ContractAddress,
        val amount: String,
        /**
         * Energy for the whole transaction including the administrative fee.
         * Legacy field.
         */
        val maxEnergy: Long?,
        /**
         * Energy for the smart contract execution only,
         * without the administrative transaction fee.
         */
        val maxContractExecutionEnergy: Long?,
        val message: String,
        val receiveName: String
    ) : Payload

    data class AccountTransaction(
        val amount: String,
        val toAddress: String
    ) : Payload
}
