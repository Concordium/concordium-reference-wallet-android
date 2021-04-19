package com.concordium.wallet.data.model

import java.io.Serializable

data class IdentityProviderInfo(
    val ipIdentity: Int,
    val ipDescription: IdentityProviderDescription,
    val ipVerifyKey: String,
    val ipCdiVerifyKey: String
) : Serializable