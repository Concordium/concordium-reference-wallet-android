package com.concordium.wallet.ui.identity.identityconfirmed

import android.app.Application
import androidx.lifecycle.*
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.identity.IdentityUpdater
import kotlinx.coroutines.launch

data class IdentityErrorData(
    val identity: Identity,
    val account: Account?,
    val isFirstIdentity: Boolean
)

class IdentityConfirmedViewModel(application: Application) : AndroidViewModel(application) {

    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository
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

    lateinit var identity: Identity

    lateinit var accountWithIdentityListLiveData: LiveData<List<AccountWithIdentity>>
    lateinit var accountWithIdentityLiveData: LiveData<AccountWithIdentity>

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
    }

    fun initialize(identity: Identity) {
        this.identity = identity
        accountWithIdentityListLiveData = accountRepository.getAllByIdentityIdWithIdentity(identity.id)
        accountWithIdentityLiveData = Transformations.map(accountWithIdentityListLiveData) { list ->
            list.firstOrNull()
        }
    }

    override fun onCleared() {
        super.onCleared()
        identityUpdater.stop()
    }

    fun startIdentityUpdate() {
        val updateListener = object: IdentityUpdater.UpdateListener {
            override fun onError(identity: Identity, account: Account?) {
                val isFirstIdentity = isFirstIdentityLiveData.value
                viewModelScope.launch {
                    val isFirst = isFirstIdentity ?: isFirst(identityRepository.getCount())
                    _identityErrorLiveData.value = IdentityErrorData(identity, account, isFirst)
                }
            }

            override fun onDone() {
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