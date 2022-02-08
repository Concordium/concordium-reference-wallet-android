package com.concordium.wallet.data.export

import java.io.Serializable


data class ExportValue(
    val identities: List<IdentityExport>,
    val recipients: List<RecipientExport>
) : Serializable {

    @Suppress("SENSELESS_COMPARISON")
    fun hasRequiredData(): Boolean {
        return identities != null
                && recipients != null
    }

    fun hasRequiredIdentities(): Boolean {
        return identities != null && identities.isNotEmpty()
    }
}