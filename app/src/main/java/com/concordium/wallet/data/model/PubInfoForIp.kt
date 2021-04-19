package com.concordium.wallet.data.model

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class PubInfoForIp(
    val idCredPub: String,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val publicKeys: RawJson,
    val regId: String
) : Serializable