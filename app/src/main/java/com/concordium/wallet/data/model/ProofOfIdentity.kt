package com.concordium.wallet.data.model

import java.io.Serializable

data class ProofOfIdentity (
    val challenge: String?,
    val statement: List<ProofOfIdentityStatement>?
): Serializable