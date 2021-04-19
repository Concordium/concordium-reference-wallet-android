package com.concordium.wallet.data.model

import java.io.Serializable

data class AttributeList(
    val chosenAttributes: HashMap<String, String>,
    val validTo: String,
    val maxAccounts: Int,
    val createdAt: String
) : Serializable