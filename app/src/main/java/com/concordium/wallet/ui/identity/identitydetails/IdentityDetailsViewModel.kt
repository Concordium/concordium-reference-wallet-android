package com.concordium.wallet.ui.identity.identitydetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.launch

class IdentityDetailsViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var identity: Identity
    private val identityRepository: IdentityRepository
    val identityChanged: MutableLiveData<Identity> by lazy { MutableLiveData<Identity>() }

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
    }

    fun initialize(identity: Identity) {
        this.identity = identity
    }

    suspend fun removeIdentity(identity: Identity) {
        identityRepository.delete(identity)
    }

    fun changeIdentityName(name: String) {
        viewModelScope.launch {
            identity.name = name
            identityRepository.update(identity)
            identityChanged.value = identity
        }
    }
}