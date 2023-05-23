package com.concordium.wallet.data.model

import java.io.Serializable

data class IdentityProvider(
    val ipInfo: IdentityProviderInfo,
    val arsInfos: Map<String, ArsInfo>,
    val metadata: IdentityProviderMetaData
) : Serializable