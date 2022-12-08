package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.model.AccountData

data class SignMessageInput(
    val address: String,
    val message: String,
    val keys: AccountData
)
