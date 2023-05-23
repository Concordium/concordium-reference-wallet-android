package com.concordium.wallet.ui.recipient.recipientlist

import android.app.Application
import androidx.lifecycle.*
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.launch

class RecipientListViewModel(application: Application) : AndroidViewModel(application) {

    var account: Account? = null
    var isShielded: Boolean = false
    private var recipientRepository: RecipientRepository
    var selectRecipientMode = false
        private set

    private val allRecipientsLiveData: LiveData<List<Recipient>>
    val recipientListLiveData: LiveData<List<Recipient>>
        get() = allRecipientsLiveData.switchMap { allRecipients ->
            val filteredRecipientsLiveData = MutableLiveData<List<Recipient>>()
            val recipientsToShowLiveData = when {
                selectRecipientMode -> {
                    val filteredList = allRecipients.filter { recipient ->
                        recipient.address != account?.address
                    }
                    filteredRecipientsLiveData.value = filteredList
                    filteredRecipientsLiveData
                }

                else -> {
                    filteredRecipientsLiveData.value = allRecipients
                    filteredRecipientsLiveData
                }
            }
            recipientsToShowLiveData
        }

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData


    init {
        val recipientDao = WalletDatabase.getDatabase(application).recipientDao()
        recipientRepository = RecipientRepository(recipientDao)
        allRecipientsLiveData = recipientRepository.allRecipients
    }

    fun initialize(
        selectRecipientMode: Boolean,
        shielded: Boolean,
        account: Account?
    ) {
        this.selectRecipientMode = selectRecipientMode
        this.isShielded = shielded
        this.account = account
    }

    fun deleteRecipient(recipient: Recipient) = viewModelScope.launch {
        recipientRepository.delete(recipient)
    }

}
