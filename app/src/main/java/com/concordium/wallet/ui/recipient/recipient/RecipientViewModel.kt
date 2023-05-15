package com.concordium.wallet.ui.recipient.recipient

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RecipientViewModel(application: Application) : AndroidViewModel(application) {
    private val cryptoLibrary = App.appCore.cryptoLibrary
    private val recipientRepository: RecipientRepository
    lateinit var recipient: Recipient
    var editRecipientMode = false
    private var selectRecipientMode: Boolean = false

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData
    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData
    private val _finishScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishScreenLiveData
    private val _gotoBackToSendFundsLiveData = MutableLiveData<Event<Boolean>>()
    val gotoBackToSendFundsLiveData: LiveData<Event<Boolean>>
        get() = _gotoBackToSendFundsLiveData

    init {
        val recipientDao = WalletDatabase.getDatabase(application).recipientDao()
        recipientRepository = RecipientRepository(recipientDao)
    }

    fun initialize(recipient: Recipient?, selectRecipientMode: Boolean) {
        this.selectRecipientMode = selectRecipientMode
        if (recipient != null) {
            this.recipient = recipient
            editRecipientMode = true
        } else {
            this.recipient = Recipient(0, "", "")
        }
    }

    fun validateRecipient(name: String, address: String): Boolean {
        return (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(address))
    }

    fun validateAndSaveRecipient(name: String, address: String): Boolean {
        val isAddressValid = cryptoLibrary.checkAccountAddress(address)
        if (!isAddressValid) {
            _errorLiveData.value = Event(R.string.recipient_error_invalid_address)
            return false
        }

        val existingRecipient = runBlocking {
            recipientRepository.getRecipientByAddress(address)
        }

        //We found an existing entry, which we should not as we are not in edit mode
        if (existingRecipient != null && !editRecipientMode) {
            _errorLiveData.value = Event(R.string.error_adding_account_duplicate)
            return false
        }

        //We found an existing entry, in edit mode, but it is not the same id - so we are creating a duplicate
        if (existingRecipient != null && editRecipientMode && existingRecipient.id != recipient.id) {
            _errorLiveData.value = Event(R.string.error_adding_account_duplicate)
            return false
        }

        recipient.name = name
        recipient.address = address
        _waitingLiveData.value = true
        saveRecipient(recipient)
        return true
    }

    private fun saveRecipient(recipient: Recipient) = viewModelScope.launch {
        if (editRecipientMode) {
            recipientRepository.update(recipient)
            val accountRepository = AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
            val existingAccount = accountRepository.findByAddress(recipient.address)
            if (existingAccount != null) {
                existingAccount.name = recipient.name
                accountRepository.update(existingAccount)
            }
        } else {
            recipientRepository.insert(recipient)
        }
        if (selectRecipientMode) {
            _gotoBackToSendFundsLiveData.value = Event(true)
        } else {
            _finishScreenLiveData.value = Event(true)
        }
    }
}
