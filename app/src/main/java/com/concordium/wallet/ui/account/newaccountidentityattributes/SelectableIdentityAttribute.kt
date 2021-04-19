package com.concordium.wallet.ui.account.newaccountidentityattributes

import java.io.Serializable

data class SelectableIdentityAttribute(
    val name: String,
    val value: String,
    var isSelected: Boolean
) : Serializable
