package com.concordium.wallet.ui.identity.identityproviderwebview

import com.walletconnect.util.Empty

enum class CreateIdentityError(val errorMessage: String = String.Empty) {
    NONE, ID_PUB("Duplicate id_cred_pub"), UNKNOWN
}
