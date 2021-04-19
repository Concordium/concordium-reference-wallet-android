package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.IdentityObject
import com.concordium.wallet.data.model.RawJson
import com.google.gson.annotations.JsonAdapter

data class GenerateAccountsInput(
    val global: GlobalParams,
    val identityObject: IdentityObject,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val privateIdObjectData: RawJson
)