package com.concordium.wallet.data.cryptolib

import java.io.Serializable

data class IdentityKeysAndRandomnessInput (
    val seed: String,
    val net: String,
    val identityIndex: Int,
): Serializable
