package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName

data class AccountDataKeys(
    @SerializedName("0")
    val level0: X0
)

data class X0(
    val keys: Keys,
    val threshold: Int
)

data class Keys(
    @SerializedName("0")
    val keys: X0X
)

data class X0X(
    val signKey: String,
    val verifyKey: String
)
