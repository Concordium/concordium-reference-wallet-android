package com.concordium.wallet.ui.recipient.scanqr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App

class ScanQRViewModel(application: Application) : AndroidViewModel(application) {

    enum class State {
        DEFAULT, NOT_VALID_QR
    }

    private val _stateLiveData = MutableLiveData<State>()
    val stateLiveData: LiveData<State>
        get() = _stateLiveData

    /**
     * Used to control return from failure/invalid QR state based on timer.
     * If there has been found a valid QR code, we do not want to go back to default scanning state.
     */
    private var allowedToReset = false

    fun initialize() {
        _stateLiveData.value = State.DEFAULT
    }

    fun setStateDefault() {
        _stateLiveData.value = State.DEFAULT
    }

    fun setStateDefaultIfAllowed() {
        if (allowedToReset && stateLiveData.value != State.DEFAULT) {
            _stateLiveData.value = State.DEFAULT
        }
    }

    fun setStateQRNotValid() {
        allowedToReset = true
        _stateLiveData.value = State.NOT_VALID_QR
    }

    fun checkQrAccountAddress(qrInfo: String): Boolean {
        return App.appCore.cryptoLibrary.checkAccountAddress(qrInfo)
    }

    fun checkQrWalletConnect(qrInfo: String): Boolean {
        return qrInfo.isNotBlank() && qrInfo.lowercase().startsWith("wc:")
    }
}
