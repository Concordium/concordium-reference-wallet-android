package com.concordium.wallet.data.model

data class Proofs(
    val challenge: String?,
    val credential: String?,
    val revealStatus: Boolean?,
    val zeroKnowledgeStatus: Boolean?,
    val proofsReveal: List<ProofReveal>?,
    val proofsZeroKnowledge: List<ProofZeroKnowledge>?
)