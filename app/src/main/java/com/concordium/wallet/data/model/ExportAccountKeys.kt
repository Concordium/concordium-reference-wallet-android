package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName

data class ExportAccountKeys(
    val type: String,
    val v: Int,
    val environment: String,
    val value: Value
)

data class Value(
    val accountKeys: AccountKeys,
    val credentials: Credentials,
    val address: String
)

data class AccountKeys(
    @SerializedName("keys")
    val accountDataKeys: AccountDataKeys,
    val threshold: Int
)

data class Credentials(
    @SerializedName("0")
    val credId: String
)
