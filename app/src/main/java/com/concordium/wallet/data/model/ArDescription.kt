package com.concordium.wallet.data.model

import java.io.Serializable

data class ArDescription(
    val url: String,
    val name: String,
    val description: String
) : Serializable