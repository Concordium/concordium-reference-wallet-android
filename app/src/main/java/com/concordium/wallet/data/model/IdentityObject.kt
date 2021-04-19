package com.concordium.wallet.data.model

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class IdentityObject(
    val attributeList: AttributeList,
    val preIdentityObject: PreIdentityObject,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val signature: RawJson
) : Serializable