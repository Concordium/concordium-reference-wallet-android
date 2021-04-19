package com.concordium.wallet.data.model

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class Credential(
    @JsonAdapter(RawJsonTypeAdapter::class)
    val arData: RawJson,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val credentialPublicKeys: RawJson,
    val ipIdentity: Long,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val policy: RawJson,
    val proofs: String,
    val regId: String,
    val revocationThreshold: Long,
    val messageExpiry: Long
) : Serializable