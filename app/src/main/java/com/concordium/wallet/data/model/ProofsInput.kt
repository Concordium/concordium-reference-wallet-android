package com.concordium.wallet.data.model

import java.io.Serializable

data class ProofsInput(
    val ipInfo: IdentityProviderInfo?,
    val global: GlobalParams?,
    val identityObject: IdentityObject?,
    val statements: List<ProofOfIdentityStatement>?,
    val challenge: ByteArray?,
    val identityIndex: Int?,
    val accountNumber: Int?,
    val seed: String?,
    val net: String?
) : Serializable