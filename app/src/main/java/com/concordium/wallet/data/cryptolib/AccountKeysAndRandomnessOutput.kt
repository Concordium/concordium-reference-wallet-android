package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.RawJson
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class AccountKeysAndRandomnessOutput (
    val signKey: String,
    val verifyKey: String,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val attributeCommitmentRandomness: RawJson
): Serializable
