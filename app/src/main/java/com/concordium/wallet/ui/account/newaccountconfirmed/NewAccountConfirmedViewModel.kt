package com.concordium.wallet.ui.account.newaccountconfirmed

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

class NewAccountConfirmedViewModel(application: Application) : AndroidViewModel(application) {

    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    lateinit var account: Account

    lateinit var accountWithIdentityLiveData: LiveData<AccountWithIdentity>

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
    }

    fun initialize(account: Account) {
        this.account = account
        accountWithIdentityLiveData = accountRepository.getByIdWithIdentityAsLiveData(account.id)
    }

    fun updateState() {
        _waitingLiveData.value = true
        viewModelScope.launch {
            _waitingLiveData.value = false

        }
    }


}