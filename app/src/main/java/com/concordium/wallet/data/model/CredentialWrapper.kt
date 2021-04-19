package com.concordium.wallet.data.model

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class CredentialWrapper(
    @JsonAdapter(RawJsonTypeAdapter::class)
    val value: RawJson,  //former Credential type
    val v: Int
) : Serializable