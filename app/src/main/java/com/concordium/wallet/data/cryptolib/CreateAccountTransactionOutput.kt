package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.RawJson
import com.google.gson.annotations.JsonAdapter

data class CreateAccountTransactionOutput(
    @JsonAdapter(RawJsonTypeAdapter::class)
    val signatures: RawJson,
    val transaction: String
)
