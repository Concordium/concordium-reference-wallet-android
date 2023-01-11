package com.concordium.wallet.data.model

import java.io.Serializable

data class ProofOutput(
    val challenge: String?,
    val idProof: Proof?,
): Serializable

data class Proof(
    val credential: String?,
    var proof: ProofSecondary?
): Serializable