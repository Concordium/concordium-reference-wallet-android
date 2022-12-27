package com.concordium.wallet.data.model

data class ProofsData(
    val challenge: String,
    val proof: ProofPrimary
)

data class ProofPrimary(
    val credential: String,
    val proof: ProofSecondary
)

data class ProofSecondary(
    val v: Int,
    val value: ValueLocal
)

data class ValueLocal(
    val proofs: List<ProofLocal>
)

data class ProofLocal(
    val proof: String,
    val attribute: String?,
    val type: String
)