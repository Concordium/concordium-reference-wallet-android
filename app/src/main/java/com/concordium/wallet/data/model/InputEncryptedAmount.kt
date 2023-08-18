package com.concordium.wallet.data.model

import java.io.Serializable

data class InputEncryptedAmount(
    val aggEncryptedAmount: String,
    val aggAmount: String,
    val aggIndex: Int
): Serializable
