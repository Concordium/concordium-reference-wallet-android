package com.concordium.wallet.data.cryptolib

data class SerializeTokenTransferParametersInput(
    val tokenId: String,
    val amount: String,
    val from: String,
    val to: String,
)
