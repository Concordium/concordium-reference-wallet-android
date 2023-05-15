package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName

enum class ShieldedAccountEncryptionStatus (val code: Int) {
    @SerializedName("decrypted")
    DECRYPTED(0),

    @SerializedName("partiallydecrypted")
    PARTIALLYDECRYPTED(1),

    @SerializedName("encrypted")
    ENCRYPTED(2)
}
