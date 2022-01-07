package com.concordium.wallet.ui.account.accountdetails

import android.app.Application
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.authentication.AuthenticationManager
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateCredentialOutput
import com.concordium.wallet.data.cryptolib.DecryptAmountInput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.data.util.toTransaction
import com.concordium.wallet.ui.account.accountdetails.transfers.AdapterItem
import com.concordium.wallet.ui.account.accountdetails.transfers.HeaderItem
import com.concordium.wallet.ui.account.accountdetails.transfers.TransactionItem
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import kotlinx.coroutines.*
import java.util.*
import javax.crypto.Cipher
import kotlin.collections.ArrayList

class AccountDetailsViewModel(application: Application) : AndroidViewModel(application) {

    fun <T> MutableLiveData<T>.forceRefresh() {
        this.value = this.value
    }

    private val session: Session = App.appCore.session

    var hasTransactionsToDecrypt: Boolean = false

    lateinit var account: Account
    var isShielded: Boolean = false

    private val proxyRepository = ProxyRepository()
    private val accountRepository: AccountRepository
    private val transferRepository: TransferRepository
    private val identityRepository: IdentityRepository
    private val recipientRepository: RecipientRepository

    private val gson = App.appCore.gson


    private lateinit var transactionMappingHelper: TransactionMappingHelper
    private val accountUpdater = AccountUpdater(application, viewModelScope)

    // Transaction state
    private var nonMergedLocalTransactions: MutableList<Transaction> = ArrayList()
    var hasMoreRemoteTransactionsToLoad = true
        private set
    private var lastRemoteTransaction: RemoteTransaction? = null
    private var isLoadingTransactions = false
    var allowScrollToLoadMore = true
    private var lastHeaderDate: Date? = null

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _newFinalizedAccountLiveData = MutableLiveData<String>()
    val newFinalizedAccountLiveData: LiveData<String>
        get() = _newFinalizedAccountLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _finishLiveData = MutableLiveData<Event<Boolean>>()
    val finishLiveData: LiveData<Event<Boolean>>
        get() = _finishLiveData

    private val _showGTUDropLiveData = MutableLiveData<Boolean>()
    val showGTUDropLiveData: LiveData<Boolean>
        get() = _showGTUDropLiveData

    private val _showPadLockLiveData = MutableLiveData<Boolean>()
    val showPadLockLiveData: LiveData<Boolean>
        get() = _showPadLockLiveData

    private var _transferListLiveData = MutableLiveData<List<AdapterItem>>()
    val transferListLiveData: LiveData<List<AdapterItem>>
        get() = _transferListLiveData

    private var _identityLiveData = MutableLiveData<Identity>()
    val identityLiveData: LiveData<Identity>
        get() = _identityLiveData

    private var _totalBalanceLiveData = MutableLiveData<Pair<Long, Boolean>>()
    val totalBalanceLiveData: LiveData<Pair<Long, Boolean>>
        get() = _totalBalanceLiveData

    private var _selectedTransactionForDecrytionLiveData = MutableLiveData<Transaction>()
    val selectedTransactionForDecrytionLiveData: LiveData<Transaction>
        get() = _selectedTransactionForDecrytionLiveData

    init {
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
        val transferDao = WalletDatabase.getDatabase(application).transferDao()
        transferRepository = TransferRepository(transferDao)
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val recipientDao = WalletDatabase.getDatabase(application).recipientDao()
        recipientRepository = RecipientRepository(recipientDao)
        initializeAccountUpdater()

    }

    fun initialize(account: Account, isShielded: Boolean) {
        this.account = account
        this.isShielded = isShielded
        getIdentityProvider()
        Log.d("Account address: ${account.address}")
    }

    override fun onCleared() {
        super.onCleared()
        accountUpdater.dispose()
    }

    fun deleteAccountAndFinish() = viewModelScope.launch {
        accountRepository.delete(account)
        _finishLiveData.value = Event(true)
    }

    private fun getIdentityProvider() {
        viewModelScope.launch {
            val identity = identityRepository.findById(account.identityId)
            _identityLiveData.value = identity
        }
    }

    fun requestGTUDrop() {
        _waitingLiveData.value = true
        proxyRepository.requestGTUDrop(
            account.address,
            {
                _waitingLiveData.value = false
                //populateTransferList()
                createGTUDropTransfer(it.submissionId)
            },
            {
                _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
                _waitingLiveData.value = false
            }
        )
    }

    private fun updateGTUDropState() {
        if (!BuildConfig.SHOW_GTU_DROP || isShielded) {
            _showGTUDropLiveData.value = false
        } else {
            _showGTUDropLiveData.value = transferListLiveData.value?.isEmpty()
        }
    }

    private fun createGTUDropTransfer(submissionId: String) {
        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000
        val createdAt = Date().time
        val transfer = Transfer(
            0,
            account.id,
            -2000000000,
            0,
            "",
            account.address,
            expiry,
            "",
            createdAt,
            submissionId,
            TransactionStatus.UNKNOWN,
            TransactionOutcome.UNKNOWN,
            TransactionType.TRANSFERTOPUBLIC,   //Not really an outgoing public transfer,
                                                //but amount is negative so it is listed as incoming positive
            null,
            0,
            null
        )
        saveTransfer(transfer)
    }

    private fun saveTransfer(transfer: Transfer) = viewModelScope.launch {
        transferRepository.insert(transfer)
        populateTransferList()
    }

    //region Transaction list update/merge
    //************************************************************

    private fun clearTransactionListState() {
        _transferListLiveData.value = null
        nonMergedLocalTransactions.clear()
        hasMoreRemoteTransactionsToLoad = true
        lastRemoteTransaction = null
        isLoadingTransactions = false
        allowScrollToLoadMore = true
        lastHeaderDate = null
    }

    fun populateTransferList(notifyWaitingLiveData: Boolean = true) {
        clearTransactionListState()
        if (account.transactionStatus == TransactionStatus.FINALIZED) {
            if(notifyWaitingLiveData){
                _waitingLiveData.value = true
            }
            viewModelScope.launch {
                accountUpdater.updateForAccount(account)
            }
        } else {
            _totalBalanceLiveData.value = Pair(0, false)
        }
    }

    private fun initializeAccountUpdater() {
        accountUpdater.setUpdateListener(object : AccountUpdater.UpdateListener {
            override fun onDone(totalBalances: TotalBalancesData) {
                _totalBalanceLiveData.value = Pair(if(isShielded) account.totalShieldedBalance else account.totalUnshieldedBalance, totalBalances.totalContainsEncrypted)
                getLocalTransfers()
            }

            override fun onNewAccountFinalized(accountName: String) {
                viewModelScope.launch {
                    _newFinalizedAccountLiveData.value = accountName
                    App.appCore.session.setAccountsBackedUp(false)
                }
            }

            override fun onError(stringRes: Int) {
                _errorLiveData.value = Event(stringRes)
            }
        })
    }

    fun getIncludeRewards(): String? {

        if (session.getHasShowRewards(account.id) && !session.getHasShowFinalizationRewards(account.id)) {
            return "allButFinalization"
        }

        if (session.getHasShowRewards(account.id) && session.getHasShowFinalizationRewards(account.id)) {
            return "all"
        }

        return "none"
    }

    fun getLocalTransfers() {
        viewModelScope.launch {
            val recipientList = recipientRepository.getAll()
            transactionMappingHelper = TransactionMappingHelper(account, recipientList)
            val transferList = transferRepository.getAllByAccountId(account.id)
            for (transfer in transferList) {
                val transaction = transfer.toTransaction()
                transactionMappingHelper.addTitlesToTransaction(transaction, transfer)
                nonMergedLocalTransactions.add(transaction)
            }
            loadRemoteTransactions(null)
        }
    }

    fun loadMoreRemoteTransactions(): Boolean {
        val lastRemote = lastRemoteTransaction
        if (isLoadingTransactions || !hasMoreRemoteTransactionsToLoad || lastRemote == null) {
            return false
        }
        loadRemoteTransactions(lastRemote.id)
        return true
    }

    private fun loadRemoteTransactions(from: Int?) {
        allowScrollToLoadMore = false
        isLoadingTransactions = true
        proxyRepository.getAccountTransactions(
            account.address,
            {
                Log.d("Got more transactions")
                hasMoreRemoteTransactionsToLoad = (it.count >= it.limit)
                if (it.transactions.isNotEmpty()) {
                    lastRemoteTransaction = it.transactions.last()
                }
                mergeTransactions(it.transactions)
                isLoadingTransactions = false
                // Its only for the initial load that it is relevant to disable loading state
                _waitingLiveData.value = false
            },
            {
                Log.d("Get more transactions failed")
                isLoadingTransactions = false
                // Its only for the initial load that it is relevant to disable loading state
                _waitingLiveData.value = false
                _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
            }, from = from, limit = 100, includeRewards = getIncludeRewards()
        )
    }

    private fun mergeTransactions(remoteTransactionList: List<RemoteTransaction>) {
        // Convert remote transactions
        val newTransactions: MutableList<Transaction> = ArrayList()
        for (remoteTransaction in remoteTransactionList) {
            val transaction = remoteTransaction.toTransaction()
            transactionMappingHelper.addTitleToTransaction(transaction, remoteTransaction)
            newTransactions.add(transaction)
        }
        // Find local transactions to merge
        val localTransactionsToBeMerged: MutableList<Transaction> = ArrayList()
        val localTransactionsToNotMerge: MutableList<Transaction> = ArrayList()
        val lastRemoteTimestamp = getLastRemoteTransactionTimestamp()
        for (ta in nonMergedLocalTransactions) {
            if (ta.timeStamp.time >= lastRemoteTimestamp) {
                localTransactionsToBeMerged.add(ta)
            } else {
                localTransactionsToNotMerge.add(ta)
            }
        }
        nonMergedLocalTransactions = localTransactionsToNotMerge
        // Merge
        newTransactions.addAll(localTransactionsToBeMerged)
        newTransactions.sortByDescending { it.timeStamp }
        // Update list with all transactions to show
        addToTransactionList(newTransactions)
    }

    private fun addToTransactionList(newTransactions: List<Transaction>) {
        val transferList = _transferListLiveData.value ?: ArrayList<AdapterItem>()
        val adapterList = transferList.toMutableList()
        for (ta in newTransactions) {
            checkToAddHeaderItem(adapterList, ta)
            adapterList.add(TransactionItem(ta))
        }
        _transferListLiveData.value = adapterList
        updateGTUDropState()
    }

    private fun checkToAddHeaderItem(
        adapterList: MutableList<AdapterItem>,
        transaction: Transaction
    ) {
        val lastDate = lastHeaderDate
        val taDate = transaction.timeStamp
        if (lastDate == null || !DateTimeUtil.isSameDay(lastDate, taDate)) {
            lastHeaderDate = taDate
            adapterList.add(HeaderItem(DateTimeUtil.formatDateAsLocalMediumWithAltTexts(taDate)))
        }
    }

    private fun getLastRemoteTransactionTimestamp(): Long {
        val lastRemote = lastRemoteTransaction
        return if (lastRemote == null) {
            0
        } else {
            lastRemote.blockTime.toLong() * 1000
        }
    }

    fun shouldUseBiometrics(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().useBiometrics()
    }

    fun getCipherForBiometrics(): Cipher? {
        try {
            val cipher = App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForDecryption()
            if (cipher == null) {
                _errorLiveData.value = Event(R.string.app_error_keystore_key_invalidated)
            }
            return cipher
        } catch (e: KeystoreEncryptionException) {
            _errorLiveData.value = Event(R.string.app_error_keystore)
            return null
        }
    }

    fun continueWithPassword(
        password: String,
        transfersOnly: Boolean = false,
        transaction: Transaction? = null
    ) = viewModelScope.launch {
        _waitingLiveData.value = true
        decryptAndContinue(password, transfersOnly, transaction)
    }


    fun checkLogin(cipher: Cipher, transfersOnly: Boolean = false, transaction: Transaction? = null) = viewModelScope.launch {
        _waitingLiveData.value = true
        val password = App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(cipher)
        if (password != null) {
            decryptAndContinue(password, transfersOnly, transaction)
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
        }
    }


    private suspend fun decryptAndContinue(
        password: String,
        transfersOnly: Boolean = false,
        transaction: Transaction? = null
    ) {
        // Decrypt the private data
        val storageAccountDataEncrypted = account.encryptedAccountData
        if (TextUtils.isEmpty(storageAccountDataEncrypted)) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return
        }
        val decryptedJson = App.appCore.getCurrentAuthenticationManager().decryptInBackground(password, storageAccountDataEncrypted)
        if (decryptedJson != null) {
            val credentialsOutput = gson.fromJson(decryptedJson, StorageAccountData::class.java)
            decryptData(credentialsOutput.encryptionSecretKey, transfersOnly, transaction)
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
        }
    }


    private suspend fun decryptData(
        secretKey: String,
        transfersOnly: Boolean = false,
        transaction: Transaction?
    ) {
        viewModelScope.launch{
            if(!transfersOnly){
                clearTransactionListState()
                accountUpdater.decryptEncryptedAmounts(secretKey, account)
                accountUpdater.decryptAllUndecryptedAmounts(secretKey)
                accountUpdater.updateForAccount(account)
            }
            else{
                if(transaction == null){
                    decryptTransactionListUnencryptedAmounts(secretKey)
                }
                else{
                    decryptTransactionUnencryptedAmounts(secretKey, transaction)
                }
            }
            _totalBalanceLiveData.value = Pair(account.totalShieldedBalance, false)
            _waitingLiveData.value = false
        }
    }


    fun initiateFrequentUpdater() {
        updater.cancel()
        updater.start()
    }

    fun stopFrequentUpdater() {
        updater.cancel()
    }

    var updater =
        object : CountDownTimer(Long.MAX_VALUE, BuildConfig.ACCOUNT_UPDATE_FREQUENCY_SEC * 1000) {
            private var first = true
            override fun onTick(millisUntilFinished: Long) {
                if(first){ //ignore first tick
                    first = false
                    return
                }
                populateTransferList(false)
            }

            override fun onFinish() {

            }
        }




    suspend fun decryptTransactionListUnencryptedAmounts(secretKey: String) {
        _transferListLiveData.value?.forEach {
            if(it.getItemType() == AdapterItem.ItemType.Item){
                val transactionItem = it as TransactionItem
                val transaction = transactionItem.transaction
                decryptTransactionUnencryptedAmounts(secretKey, transaction)
            }
        }
    }

    suspend fun decryptTransactionUnencryptedAmounts(secretKey: String, transaction: Transaction?) {
        GlobalScope.launch(Dispatchers.IO) {
            transaction?.let {
                if (it.encrypted != null) {

                    if(it.encrypted.encryptedAmount != null && accountUpdater.lookupMappedAmount(it.encrypted.encryptedAmount) == null){

                        if (it.encrypted.newSelfEncryptedAmount != null && it.details?.inputEncryptedAmount != null) {
                            var newSelfAmount = 0L
                            var inputAmount = 0L
                            if (it.encrypted.newSelfEncryptedAmount != null) {
                                val output = App.appCore.cryptoLibrary.decryptEncryptedAmount(
                                    DecryptAmountInput(
                                        it.encrypted.newSelfEncryptedAmount,
                                        secretKey
                                    )
                                )
                                if (output != null) {
                                    newSelfAmount = output.toLong()
                                }
                            }
                            if (it.details?.inputEncryptedAmount != null) {
                                val output = App.appCore.cryptoLibrary.decryptEncryptedAmount(
                                    DecryptAmountInput(
                                        it.details?.inputEncryptedAmount,
                                        secretKey
                                    )
                                )
                                if (output != null) {
                                    inputAmount = output.toLong()
                                }
                            }
                            it.encrypted.encryptedAmount?.let {
                                accountUpdater.saveDecryptedAmount(it, (-(newSelfAmount - inputAmount)).toString())
                                GlobalScope.launch(Dispatchers.Main) {
                                    _transferListLiveData.forceRefresh()
                                }
                            }
                        }
                        else
                        if (it.encrypted?.encryptedAmount != null) {
                            val output =
                                App.appCore.cryptoLibrary.decryptEncryptedAmount(DecryptAmountInput(it.encrypted.encryptedAmount, secretKey))
                            if (output != null) {
                                accountUpdater.saveDecryptedAmount(it.encrypted.encryptedAmount, output)
                                GlobalScope.launch(Dispatchers.Main) {
                                    _transferListLiveData.forceRefresh()
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    fun selectTransactionForDecryption(ta: Transaction) {
        _selectedTransactionForDecrytionLiveData.value = ta
    }

    fun usePasscode(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().usePasscode()
    }

    fun checkForUndecryptedAmounts() {
        GlobalScope.launch(Dispatchers.IO) {
            var showPadlock = false
            _transferListLiveData.value?.forEach {
                if(it.getItemType() == AdapterItem.ItemType.Item){
                    val transactionItem = it as TransactionItem
                    val transaction = transactionItem.transaction
                    if(transaction != null && transaction.encrypted != null && transaction.encrypted.encryptedAmount != null){
                        if(accountUpdater.lookupMappedAmount(transaction.encrypted.encryptedAmount) == null){
                            showPadlock = true
                        }
                    }
                }
            }
            hasTransactionsToDecrypt = showPadlock
            GlobalScope.launch(Dispatchers.Main) {
                _showPadLockLiveData.value = showPadlock
            }
        }
    }



    // endregion

}