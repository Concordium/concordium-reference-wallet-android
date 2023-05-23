package com.concordium.wallet.data.cryptolib

import java.io.Serializable

data class IdentityKeysAndRandomnessOutput(
    val idCredSec: String,
    val prfKey: String,
    val blindingRandomness: String
) : Serializable
