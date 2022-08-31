package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.RawJson
import com.google.gson.annotations.JsonAdapter

data class IdRequestAndPrivateDataOutputV1(
    @JsonAdapter(RawJsonTypeAdapter::class)
    val idObjectRequest: RawJson
)
