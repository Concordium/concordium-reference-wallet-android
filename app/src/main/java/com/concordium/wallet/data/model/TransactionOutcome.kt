package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName

enum class TransactionOutcome(val code: Int) {
    @SerializedName("success")
    Success(0),

    @SerializedName("reject")
    Reject(1),

    @SerializedName("ambiguous")
    Ambiguous(2),

    // This has been added to have a default value
    @SerializedName("unknown")
    UNKNOWN(-1)
}