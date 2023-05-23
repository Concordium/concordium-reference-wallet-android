package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.IdentityProviderInfo

data class GenerateRecoveryRequestInput(
    val ipInfo: IdentityProviderInfo,
    val global: GlobalParams,
    val seed: String,
    val net: String,
    val identityIndex: Int,
    val timestamp: Long?
)
