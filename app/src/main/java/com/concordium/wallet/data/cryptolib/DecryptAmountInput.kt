package com.concordium.wallet.data.cryptolib


data class DecryptAmountInput(
    val encryptedAmount: String,
    val encryptionSecretKey: String
)