package com.concordium.wallet.data.model

import java.io.Serializable

data class ArsInfo(
    val arIdentity: Int,
    val arPublicKey: String,
    val arDescription: ArDescription
) : Serializable