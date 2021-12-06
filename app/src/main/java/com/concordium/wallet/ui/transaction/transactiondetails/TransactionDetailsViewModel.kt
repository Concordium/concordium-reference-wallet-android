package com.concordium.wallet.ui.transaction.transactiondetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionSource
import com.concordium.wallet.data.model.TransferSubmissionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import kotlinx.coroutines.runBlocking

class TransactionDetailsViewModel(application: Application) : AndroidViewModel(application) {

    var isShieldedAccount: Boolean = false
    private val proxyRepository = ProxyRepository()
    private val recipientRepository: RecipientRepository

    private val transferRepository: TransferRepository
    private val gson = App.appCore.gson

    private var transferSubmissionStatusRequest: BackendRequest<TransferSubmissionStatus>? = null
    lateinit var account: Account
    lateinit var transaction: Transaction

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _showDetailsLiveData = MutableLiveData<Event<Boolean>>()
    val showDetailsLiveData: LiveData<Event<Boolean>>
        get() = _showDetailsLiveData

    init {
        val transferDao = WalletDatabase.getDatabase(application).transferDao()
        transferRepository = TransferRepository(transferDao)
        val recipientDao = WalletDatabase.getDatabase(application).recipientDao()
        recipientRepository = RecipientRepository(recipientDao)

    }

    fun initialize(account: Account, transaction: Transaction) {
        this.account = account
        this.transaction = transaction
    }

    override fun onCleared() {
        super.onCleared()
        transferSubmissionStatusRequest?.dispose()
    }

    fun showData() {
        if (transaction.source == TransactionSource.Local) {
            // Update the local transaction before showing it
            transaction.submissionId?.let {
                _waitingLiveData.value = true
                loadTransferSubmissionStatus(it)
                return

            }
            _waitingLiveData.value = false
        } else {
            _waitingLiveData.value = false
            _showDetailsLiveData.value = Event(true)
        }
    }

    private fun loadTransferSubmissionStatus(submissionId: String) {
        _waitingLiveData.value = true
        transferSubmissionStatusRequest?.dispose()
        transferSubmissionStatusRequest = proxyRepository.getTransferSubmissionStatus(submissionId,
            {
                // Update the transaction - the changes are not saved (they will be updated elsewhere)
                transaction.transactionHash = it.transactionHash
                transaction.blockHashes = it.blockHashes
                transaction.transactionStatus = it.status
                transaction.outcome = it.outcome ?: TransactionOutcome.UNKNOWN
                transaction.rejectReason = it.rejectReason
                _waitingLiveData.value = false
                _showDetailsLiveData.value = Event(true)
            },
            {
                _waitingLiveData.value = false
                _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
            }
        )

    }

    fun setIsShieldedAccount(shielded: Boolean) {
        isShieldedAccount  = shielded
    }

    fun addressLookup(address: String?, defaultTitle: String?): Any? {
        if(address == null){
            return defaultTitle
        }
        return runBlocking {
            recipientRepository.getRecipientByAddress(address)?.let { it.name }
        } ?: defaultTitle
    }

}