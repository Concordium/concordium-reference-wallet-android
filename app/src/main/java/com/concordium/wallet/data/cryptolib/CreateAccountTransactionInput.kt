package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.walletconnect.Payload

data class CreateAccountTransactionInput(
    val expiry: Int,
    val from: String,
    val keys: AccountData,
    val nonce: Int,
    val payload: Payload,
    val type: String
)