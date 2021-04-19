package com.concordium.wallet.data.export

import java.io.Serializable

data class RecipientExport(
    val name: String,
    val address: String
) : Serializable