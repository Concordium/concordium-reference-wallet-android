package com.concordium.wallet.data.model

import java.io.Serializable

data class TransactionEncrypted(
    val newStartIndex: Int?,
    val newSelfEncryptedAmount: String?,
    val encryptedAmount: String?,
    val newIndex: Int?
) : Serializable {

}
