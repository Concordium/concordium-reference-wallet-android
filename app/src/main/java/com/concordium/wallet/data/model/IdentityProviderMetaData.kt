package com.concordium.wallet.data.model

import java.io.Serializable

data class IdentityProviderMetaData(
    val icon: String,
    val issuanceStart: String,
    val support: String?,
    val recoveryStart: String?

) : Serializable {
    fun getSupportWithDefault(): String {
        return support ?: "support@concordium.software"
    }
}