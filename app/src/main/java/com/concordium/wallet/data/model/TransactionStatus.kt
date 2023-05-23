package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName

enum class TransactionStatus(val code: Int) {
    @SerializedName("received")
    RECEIVED(0),

    @SerializedName("absent")
    ABSENT(1),

    @SerializedName("committed")
    COMMITTED(2),

    @SerializedName("finalized")
    FINALIZED(3),

    // This has been added to have a default value
    @SerializedName("unknown")
    UNKNOWN(-1)
}