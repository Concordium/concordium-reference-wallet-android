package com.concordium.wallet.data.model

import java.io.Serializable

data class IdentityAttribute(
    val name: String,
    val value: String
) : Serializable
