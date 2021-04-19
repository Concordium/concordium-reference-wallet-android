package com.concordium.wallet.data.model

data class IdentityTokenContainer(
    val status: String,
    val token: IdentityToken?,
    val detail: String?
)