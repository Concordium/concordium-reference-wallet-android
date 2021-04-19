package com.concordium.wallet.data.model

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.RawJson
import com.google.gson.annotations.JsonAdapter

data class IdentityRequest(
    @JsonAdapter(RawJsonTypeAdapter::class)
    val idObjectRequest: RawJson
)