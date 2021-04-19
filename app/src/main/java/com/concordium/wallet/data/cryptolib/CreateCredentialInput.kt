package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.*
import com.google.gson.JsonArray
import com.google.gson.annotations.JsonAdapter

data class CreateCredentialInput(
    val identityObject: IdentityObject,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val privateIdObjectData: RawJson,
    val global: GlobalParams,
    val ipInfo: IdentityProviderInfo,
    val revealedAttributes: JsonArray,
    val accountNumber: Int,
    val arsInfos: Map<String, ArsInfo>,
    val expiry: Long?

)