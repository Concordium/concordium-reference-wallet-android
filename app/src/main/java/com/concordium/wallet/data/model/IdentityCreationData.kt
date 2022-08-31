package com.concordium.wallet.data.model

import java.io.Serializable

data class IdentityCreationData(
    val identityProvider: IdentityProvider,
    val idObjectRequest: RawJson,
    val identityName: String,
    val identityIndex: Int
) : Serializable
