package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName

enum class TransactionType(val type: Int) {
    @SerializedName("transfer")
    TRANSFER(0),

    @SerializedName("encryptedAmountTransfer")
    ENCRYPTEDAMOUNTTRANSFER(1),

    @SerializedName("transferToEncrypted")
    TRANSFERTOENCRYPTED(2),

    @SerializedName("transferToPublic")
    TRANSFERTOPUBLIC(3),

    @SerializedName("blockReward")
    BLOCKREWARD(4),

    @SerializedName("finalizationReward")
    FINALIZATIONREWARD(5),

    @SerializedName("bakingReward")
    BAKINGREWARD(6),

    @SerializedName("transferWithMemo")
    TRANSFERWITHMEMO(7),

    @SerializedName("encryptedAmountTransferWithMemo")
    ENCRYPTEDAMOUNTTRANSFERWITHMEMO(8),

    @SerializedName("delegation")
    LOCAL_DELEGATION(1001),

    @SerializedName("baker")
    LOCAL_BAKER(1002),

    // This has been added to have a default value
    @SerializedName("unknown")
    UNKNOWN(-1)
}

