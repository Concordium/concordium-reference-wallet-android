package com.concordium.wallet.data.model

import java.io.Serializable

data class ProofsData(
    val idProof: ProofSecondary
) : Serializable

data class ProofSecondary(
    val v: Int,
    val value: ValueLocal
) : Serializable

data class ValueLocal(
    val proofs: List<ProofLocal>
) : Serializable

data class ProofLocal(
    val proof: String,
    val attribute: String?,
    val type: String?,
    val lower: String?,
    val upper: String?,
    val set: List<String>?
) : Serializable