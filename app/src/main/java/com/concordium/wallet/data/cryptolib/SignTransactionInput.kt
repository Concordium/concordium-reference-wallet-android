package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.model.AccountData

data class SignTransactionInput(
    val transaction: String,
    val keys: AccountData
)
