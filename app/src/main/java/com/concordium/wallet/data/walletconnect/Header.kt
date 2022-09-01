package com.concordium.wallet.data.walletconnect

data class Header(
    val expiry: String,
    val nonce: String,
    val sender: String
)