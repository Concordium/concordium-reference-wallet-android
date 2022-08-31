package com.concordium.wallet.ui.identity.identityproviderpolicywebview

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class IdentityProviderPolicyWebViewViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var url: String

    fun initialize(url: String) {
        this.url = url
    }
}
