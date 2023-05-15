package com.concordium.wallet.ui.account.accountdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.launch

class AccountSettingsViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var account: Account
    var isShielded: Boolean = false

    val accountUpdated: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val shieldingEnabledLiveData: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    fun initialize(account: Account, isShielded: Boolean) {
        this.account = account
        this.isShielded = isShielded
        shieldingEnabledLiveData.value = App.appCore.session.isShieldingEnabled(account.address)
    }

    fun changeAccountName(name: String) {
        val accountRepository = AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
        val recipientRepository = RecipientRepository(WalletDatabase.getDatabase(getApplication()).recipientDao())
        viewModelScope.launch {
            account.name = name
            accountRepository.update(account)
            recipientRepository.getRecipientByAddress(account.address)?.let { recipient ->
                recipient.name = name
                recipientRepository.update(recipient)
            }
            accountUpdated.postValue(true)
        }
    }

    fun enableShielded() {
        App.appCore.session.setShieldingEnabled(account.address, true)
        shieldingEnabledLiveData.postValue(true)
    }

    fun disableShielded() {
        App.appCore.session.setShieldingEnabled(account.address, false)
        shieldingEnabledLiveData.postValue(false)
    }
}
