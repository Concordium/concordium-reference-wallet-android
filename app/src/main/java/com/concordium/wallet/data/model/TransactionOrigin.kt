package com.concordium.wallet.data.model

import java.io.Serializable

data class TransactionOrigin(
    val type: TransactionOriginType,
    val address: String?
) : Serializable