package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName

enum class TransactionOriginType(val code: Int) {
    @SerializedName("self")
    Self(0),

    @SerializedName("account")
    Account(1),

    @SerializedName("reward")
    Reward(2),

    @SerializedName("none")
    None(3),

    // This has been added to have a default value
    @SerializedName("unknown")
    UNKNOWN(-1)
}