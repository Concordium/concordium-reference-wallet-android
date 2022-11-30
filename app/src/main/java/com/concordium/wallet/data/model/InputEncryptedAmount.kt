package com.concordium.wallet.data.model

data class InputEncryptedAmount(
    val aggEncryptedAmount: String,
    val aggAmount: String,
    val aggIndex: Int
): java.io.Serializable