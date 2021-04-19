package com.concordium.wallet.ui.identity.identitiesoverview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase


class IdentitiesOverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val identityRepository: IdentityRepository
    val identityListLiveData: LiveData<List<Identity>>

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        identityListLiveData = identityRepository.allIdentities
    }

    fun initialize() {

    }

}