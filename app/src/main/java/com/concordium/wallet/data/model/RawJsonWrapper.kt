package com.concordium.wallet.data.model

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.annotations.JsonAdapter

data class RawJsonWrapper(
    @JsonAdapter(RawJsonTypeAdapter::class)
    val value: RawJson,
    val v: Int
)