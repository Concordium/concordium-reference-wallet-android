package com.concordium.wallet.ui.account.earn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.ChainParameters
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log

class EarnViewModel(application: Application) : AndroidViewModel(application) {
    private val proxyRepository = ProxyRepository()

    val chainParameters: MutableLiveData<ChainParameters> by lazy { MutableLiveData<ChainParameters>() }
    val error: MutableLiveData<Event<Int>> by lazy { MutableLiveData<Event<Int>>() }

    fun loadChainParameters() {
        proxyRepository.getChainParameters(
            {
                chainParameters.value = it
            },
            {
                handleBackendError(it)
            }
        )
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        error.value = Event(BackendErrorHandler.getExceptionStringRes(throwable))
    }
}
