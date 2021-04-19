package com.concordium.wallet.data.model

import java.io.Serializable

data class AccountEncryptedAmount(
    val incomingAmounts: Array<String>,
    val selfAmount: String,
    var selfAmountDecrypted: Long,
    val startIndex: Int
) : Serializable
