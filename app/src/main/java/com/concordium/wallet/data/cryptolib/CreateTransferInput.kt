package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.*
import com.google.gson.annotations.JsonAdapter

data class CreateTransferInput(
    val from: String,
    val keys: AccountData,
    val to: String,
    val expiry: Long,
    val amount: String,
    val energy: Long,
    val nonce: Int,
    val global: GlobalParams?,
    val receiverPublicKey: String?,
    val senderSecretKey: String?,
    val inputEncryptedAmount: InputEncryptedAmount?
)