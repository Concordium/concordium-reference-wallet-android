package com.concordium.wallet.ui.bakerdelegation.baker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.model.BakerData
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.util.Log
import kotlinx.coroutines.launch

class BakerViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var bakerData: BakerData
    private val transferRepository: TransferRepository

    init {
        val transferDao = WalletDatabase.getDatabase(application).transferDao()
        transferRepository = TransferRepository(transferDao)
    }

    fun initialize(bakerData: BakerData) {
        this.bakerData = bakerData
    }

    fun selectOpenBaker() {
        bakerData.isOpenBaker = true
        bakerData.isClosedBaker = false
    }

    fun selectClosedBaker() {
        bakerData.isOpenBaker = false
        bakerData.isClosedBaker = true
    }

    fun isOpenBaker(): Boolean {
        return bakerData.account?.accountBaker?.bakerId == null
    }

    fun isClosedBaker(): Boolean {
        return bakerData.account?.accountBaker?.bakerId != null
    }

    fun generateKeys() {

        viewModelScope.launch {
            // _waitingLiveData.value = true
            val bakerKeys = App.appCore.cryptoLibrary.generateBakerKeys()
            Log.d(bakerKeys.toString())
        }
    }
}
