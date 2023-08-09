package com.concordium.wallet.ui.identity.identityproviderwebview

enum class CreateIdentityError(val statusList: List<String> = emptyList()) {
    NONE(listOf("code:200")),
    ID_PUB(
        listOf(
            "Duplicate id_cred_pub",
            "idCredPub already exists"
        )
    ),
    UNKNOWN
}

fun String?.containsStatusError(statusError: CreateIdentityError): Boolean {
    if (this == null) return false

    statusError.statusList.forEach {
        if (contains(it)) return true
    }
    return false
}
