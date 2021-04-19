package com.concordium.wallet.data.model

import java.io.Serializable

data class AccountNonce(
    val nonce: Int,
    val allFinal: Boolean
) : Serializable