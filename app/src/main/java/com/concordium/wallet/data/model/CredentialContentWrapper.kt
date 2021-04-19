package com.concordium.wallet.data.model

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class CredentialContentWrapper(
    @JsonAdapter(RawJsonTypeAdapter::class)
    val credential: RawJson,
    val messageExpiry: Long = 0
) : Serializable