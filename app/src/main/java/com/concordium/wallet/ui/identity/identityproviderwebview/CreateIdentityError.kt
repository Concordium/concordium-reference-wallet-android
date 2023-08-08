package com.concordium.wallet.ui.identity.identityproviderwebview

enum class CreateIdentityError(val statusList: List<String> = emptyList()) {
    NONE(listOf("code:200")), ID_PUB(
        listOf(
            "Duplicate id_cred_pub",
            "idCredPub already exists"
        )
    ),
    UNKNOWN
}

fun CreateIdentityError.containsStatusMessage(response: String?): Boolean {
    if (response == null) return false

    statusList.forEach {
        if (response.contains(it)) return true
    }
    return false
}
