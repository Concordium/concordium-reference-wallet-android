package com.concordium.wallet.ui.identity.identitycreate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.launch

class IdentityCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val identityRepository: IdentityRepository

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _isFirstIdentityLiveData = MutableLiveData<Boolean>()

//    var customAccountName: String? = null

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
    }

    fun initialize() {

    }

    fun updateState() {
        _waitingLiveData.value = true
        viewModelScope.launch {
            val identityCount = identityRepository.getCount()
            _waitingLiveData.value = false
            _isFirstIdentityLiveData.value = (identityCount == 0)
        }
    }

}