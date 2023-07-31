package com.concordium.wallet.ui.identity.identityproviderwebview

import com.walletconnect.util.Empty

enum class CreateIdentityError(val message: String = String.Empty) {
    NONE("code:200"), ID_PUB("Duplicate id_cred_pub"), UNKNOWN
}
