package com.concordium.wallet.ui.common.failed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.concordium.wallet.core.backend.BackendError


class FailedViewModel(application: Application) : AndroidViewModel(application) {

    enum class Source {
        Identity, Account, Transfer
    }

    lateinit var source: Source
    var error: BackendError? = null

    fun initialize(source: Source, error: BackendError?) {
        this.source = source
        this.error = error
    }
}