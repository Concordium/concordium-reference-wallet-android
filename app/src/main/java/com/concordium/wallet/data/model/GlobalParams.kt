package com.concordium.wallet.data.model

data class GlobalParams(
    val onChainCommitmentKey: String,
    val bulletproofGenerators: String,
    val genesisString: String
)