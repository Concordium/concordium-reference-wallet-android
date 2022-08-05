package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.model.ArsInfo
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.IdentityProviderInfo

data class IdRequestAndPrivateDataInputV1(
    val ipInfo: IdentityProviderInfo,
    val global: GlobalParams?,
    val arsInfos: Map<String, ArsInfo>,
    val seed: String,
    val net: String,
    val identityIndex: Int
)
