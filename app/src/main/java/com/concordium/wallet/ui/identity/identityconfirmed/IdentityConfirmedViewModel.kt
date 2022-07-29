package com.concordium.wallet.ui.identity.identityconfirmed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.identity.IdentityUpdater
import kotlinx.coroutines.launch

data class IdentityErrorData(
    val identity: Identity,
    val isFirstIdentity: Boolean
)

class IdentityConfirmedViewModel(application: Application) : AndroidViewModel(application) {
    private val identityRepository: IdentityRepository
    private val identityUpdater = IdentityUpdater(application, viewModelScope)

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _isFirstIdentityLiveData = MutableLiveData<Boolean>()
    val isFirstIdentityLiveData: LiveData<Boolean>
        get() = _isFirstIdentityLiveData

    private val _identityErrorLiveData = MutableLiveData<IdentityErrorData>()
    val identityErrorLiveData: LiveData<IdentityErrorData>
        get() = _identityErrorLiveData

    private val _identityDoneLiveData = MutableLiveData<Boolean>()
    val identityDoneLiveData: LiveData<Boolean>
        get() = _identityDoneLiveData

    lateinit var identity: Identity

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
    }

    override fun onCleared() {
        super.onCleared()
        identityUpdater.stop()
    }

    fun startIdentityUpdate() {
        val updateListener = object: IdentityUpdater.UpdateListener {
            override fun onError(identity: Identity) {
                val isFirstIdentity = isFirstIdentityLiveData.value
                viewModelScope.launch {
                    val isFirst = isFirstIdentity ?: isFirst(identityRepository.getCount())
                    _identityErrorLiveData.value = IdentityErrorData(identity, isFirst)
                }
            }
            override fun onDone() {
                _identityDoneLiveData.value = true
            }
        }
        identityUpdater.checkPendingIdentities(updateListener)
    }

    fun updateState() {
        _waitingLiveData.value = true
        viewModelScope.launch {
            val identityCount = identityRepository.getCount()
            _waitingLiveData.value = false
            // If we are in the process of creating the first identity, there will be one identity saved at this point
            _isFirstIdentityLiveData.value = isFirst(identityCount)
        }
    }

    private fun isFirst(identityCount: Int): Boolean {
        // If we are in the process of creating the first identity, there will be one identity saved at this point
        return (identityCount <= 1)
    }
}
