package com.concordium.wallet.data.model

import java.io.Serializable

data class GlobalParams(
    val onChainCommitmentKey: String,
    val bulletproofGenerators: String,
    val genesisString: String
): Serializable
