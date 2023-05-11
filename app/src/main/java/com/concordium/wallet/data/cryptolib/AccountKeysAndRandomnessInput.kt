package com.concordium.wallet.data.cryptolib

import java.io.Serializable

data class AccountKeysAndRandomnessInput(
    val seed: String,
    val net: String,
    val identityIndex: Int,
    val accountCredentialIndex: Int
) : Serializable
