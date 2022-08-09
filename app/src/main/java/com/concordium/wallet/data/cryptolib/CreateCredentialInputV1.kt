package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.model.ArsInfo
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.IdentityObject
import com.concordium.wallet.data.model.IdentityProviderInfo
import com.google.gson.JsonArray

data class CreateCredentialInputV1(
    val ipInfo: IdentityProviderInfo,
    val arsInfos: Map<String, ArsInfo>,
    val global: GlobalParams,
    val identityObject: IdentityObject,
    val revealedAttributes: JsonArray,
    val seed: String,
    val net: String,
    val identityIndex: Int,
    val accountNumber: Int,
    val expiry: Long?
)