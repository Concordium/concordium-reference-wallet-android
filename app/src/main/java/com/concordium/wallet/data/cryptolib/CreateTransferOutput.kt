package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.RawJson
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class CreateTransferOutput(
    @JsonAdapter(RawJsonTypeAdapter::class)
    val signatures: RawJson,
    val addedSelfEncryptedAmount: String?,
    val remaining: String?,
    val transaction: String
) : Serializable
