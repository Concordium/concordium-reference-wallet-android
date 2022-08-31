package com.concordium.wallet.ui.identity.identitiesoverview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.launch

class IdentitiesOverviewViewModel(application: Application) : AndroidViewModel(application) {
    private val identityRepository: IdentityRepository

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _identityListLiveData = MutableLiveData<List<Identity>>()
    val identityListLiveData: LiveData<List<Identity>>
        get() = _identityListLiveData

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
    }

    fun loadIdentities() {
        viewModelScope.launch {
            _identityListLiveData.value = identityRepository.getAll()
        }
    }
}
