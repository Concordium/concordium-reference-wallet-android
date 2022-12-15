package com.concordium.wallet.data.model

data class Proofs(
    val proofsReveal: List<ProofReveal>?,
    val proofsZeroKnowledge: List<ProofZeroKnowledge>?
)