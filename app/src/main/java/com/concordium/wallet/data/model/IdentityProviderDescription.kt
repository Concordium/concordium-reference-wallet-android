package com.concordium.wallet.data.model

import java.io.Serializable

data class IdentityProviderDescription(
    val description: String,
    val name: String,
    val url: String
) : Serializable