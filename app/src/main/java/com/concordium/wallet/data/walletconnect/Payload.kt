package com.concordium.wallet.data.walletconnect

import java.math.BigInteger

sealed interface Payload {

    data class ContractUpdateTransaction(
        val address: ContractAddress,
        val amount: String,
        var maxEnergy: BigInteger,
        val message: String,
        val receiveName: String
    ) : Payload

    data class AccountTransaction(
        val amount: String,
        val toAddress: String
    ) : Payload

}

