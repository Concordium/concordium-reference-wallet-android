package com.concordium.wallet.data.model

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.annotations.JsonAdapter

data class AccountData(
    @JsonAdapter(RawJsonTypeAdapter::class)
    val keys: RawJson,
    val threshold: Int
) {

}


