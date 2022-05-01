package com.concordium.wallet.ui.account.common.accountupdater

import android.app.Application
import android.text.TextUtils
import com.concordium.wallet.App
import com.concordium.wallet.core.backend.BackendErrorException
import com.concordium.wallet.core.backend.ErrorParser
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.EncryptedAmountRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.DecryptAmountInput
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.*
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.PerformanceUtil
import kotlinx.coroutines.*
import retrofit2.HttpException

class AccountUpdater(val application: Application, private val viewModelScope: CoroutineScope) {

    companion object {
        const val DEFAULT_EMPTY_ENCRYPTED_AMOUNT = "c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
    }

    data class AccountSubmissionStatusRequestData(
        val deferred: Deferred<AccountSubmissionStatus>,
        val account: Account
    )

    data class TransferSubmissionStatusRequestData(
        val deferred: Deferred<TransferSubmissionStatus>,
        val transfer: Transfer
    )

    data class AccountBalanceRequestData(
        val deferred: Deferred<AccountBalance>,
        val account: Account
    )

    private val proxyRepository = ProxyRepository()
    private val accountRepository: AccountRepository
    private val encryptedAmountRepository: EncryptedAmountRepository
    private val transferRepository: TransferRepository
    private val recipientRepository: RecipientRepository

    private var accountSubmissionStatusRequestList: MutableList<AccountSubmissionStatusRequestData> =
        ArrayList()
    private var transferSubmissionStatusRequestList: MutableList<TransferSubmissionStatusRequestData> =
        ArrayList()
    private var accountBalanceRequestList: MutableList<AccountBalanceRequestData> =
        ArrayList()

    private var primaryJob: Job? = null
    private var updateListener: UpdateListener? = null
    private var accountList: MutableList<Account> = ArrayList()
    private var transferList: MutableList<Transfer> = ArrayList()
    private var transfersToDeleteList: MutableList<Transfer> = ArrayList()

    init {
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
        val transferDao = WalletDatabase.getDatabase(application).transferDao()
        transferRepository = TransferRepository(transferDao)
        val encryptedAmountDao = WalletDatabase.getDatabase(application).encryptedAmountDao()
        encryptedAmountRepository = EncryptedAmountRepository(encryptedAmountDao)
        val recipientDao = WalletDatabase.getDatabase(application).recipientDao()
        recipientRepository = RecipientRepository(recipientDao)

    }

    interface UpdateListener {
        fun onError(stringRes: Int)
        fun onDone(totalBalances: TotalBalancesData)
        fun onNewAccountFinalized(accountName: String)
    }

    fun setUpdateListener(updateListener: UpdateListener) {
        this.updateListener = updateListener
    }

    fun dispose() {
        // Coroutines is disposed/cancelled when in viewModelScope
    }

    private fun reset() {
        accountList.clear()
        transferList.clear()
        transfersToDeleteList.clear()

        // Cancel coroutines
        primaryJob?.cancel()
        for (request in accountSubmissionStatusRequestList) {
            request.deferred.cancel()
        }
        accountSubmissionStatusRequestList.clear()
        for (request in transferSubmissionStatusRequestList) {
            request.deferred.cancel()
        }
        transferSubmissionStatusRequestList.clear()
        for (request in accountBalanceRequestList) {
            request.deferred.cancel()
        }
        accountBalanceRequestList.clear()
    }

    fun updateForAllAccounts() {
        PerformanceUtil.showDeltaTime()
        reset()
        viewModelScope.launch {
            // Get all accounts
            accountList = accountRepository.getAll().toMutableList()
            runUpdate()
        }
    }

    fun updateForAccount(account: Account) {
        PerformanceUtil.showDeltaTime()
        reset()
        viewModelScope.launch {
            accountList.add(account)
            runUpdate()
        }
    }

    private fun runUpdate() {
        primaryJob = viewModelScope.launch {
            try {
                val accountSubStatesjob = loadAccountSubmissionStatusForAll()
                Log.d("Waiting for all account sub states to load")
                accountSubStatesjob.join()
                Log.d("Continuing after loading account substates")
                getTransfers()
                val transferSubStatesJob = loadTransferSubmissionStatusForAll()
                Log.d("Waiting for all transfer sub states to load")
                transferSubStatesJob.join()
                Log.d("Continuing after loading transfer substates")
                saveAndDeleteTransfers()
                val accountBalancesJob = loadAccountBalancesForAll()
                Log.d("Waiting for all account balances to load")
                accountBalancesJob.join()
                Log.d("Continuing after loading account balances")
                val totalBalances = calculateTotalBalances()
                saveAccounts()
                PerformanceUtil.showDeltaTime("Account updater run")
                updateListener?.onDone(totalBalances)
            } catch (e: Exception) {
                Log.e("Exception in primary job")
            }
        }
    }

    private fun loadAccountSubmissionStatusForAll(): Job = viewModelScope.launch {
        Log.d("start")
        supervisorScope {
            try {
                val accountListCloned = accountList.toMutableList() // prevent ConcurrentModificationException
                for (account in accountListCloned) {
                    if ((account.transactionStatus == TransactionStatus.COMMITTED
                                || account.transactionStatus == TransactionStatus.RECEIVED
                                || account.transactionStatus == TransactionStatus.UNKNOWN) && !TextUtils.isEmpty(account.submissionId)
                    ) {
                        val deferred = async {
                            proxyRepository.getAccountSubmissionStatusSuspended(account.submissionId)
                        }
                        val requestData = AccountSubmissionStatusRequestData(
                            deferred,
                            account
                        )
                        accountSubmissionStatusRequestList.add(requestData)
                    }
                }

                val accountSubmissionStatusRequestListCloned = accountSubmissionStatusRequestList.toMutableList() // prevent ConcurrentModificationException
                for (request in accountSubmissionStatusRequestListCloned) {
                    Log.d("AccountSubmissionStatus Loop item start")
                    val submissionStatus = request.deferred.await()

                    //If we change state to finalized we save it in address book
                    if(request.account.transactionStatus != submissionStatus.status && submissionStatus.status == TransactionStatus.FINALIZED){
                        viewModelScope.launch(Dispatchers.Default) {


                            updateListener?.onNewAccountFinalized(request.account.name)
                            recipientRepository.insert(Recipient(0, request.account.name, request.account.address))
                        }
                    }

                    request.account.transactionStatus = submissionStatus.status
                    Log.d("AccountSubmissionStatus Loop item end - ${request.account.submissionId} ${submissionStatus.status}")
                }
            } catch (e: Exception) {
                Log.e("AccountSubmissionStates failed", e)
                handleBackendError(e)
            }
        }
        Log.d("end")
    }

    private suspend fun getTransfers() {
        Log.d("start")
        if (accountList.size == 1) {
            getTransfersForAccount(accountList.first())
        } else {
            getAllTransfers()
        }
        Log.d("end")
    }

    private suspend fun getTransfersForAccount(account: Account) {
        val transfers = transferRepository.getAllByAccountId(account.id)
        transferList = transfers.toMutableList()
    }

    private suspend fun getAllTransfers() {
        val transfers = transferRepository.getAll()
        transferList = transfers.toMutableList()
    }

    private fun loadTransferSubmissionStatusForAll() = viewModelScope.launch {
        Log.d("start")
        supervisorScope {
            try {
                for (transfer in transferList) {
                    if (transfer.transactionStatus == TransactionStatus.COMMITTED
                        || transfer.transactionStatus == TransactionStatus.RECEIVED
                        || transfer.transactionStatus == TransactionStatus.UNKNOWN
                    ) {
                        val deferred = async {
                            proxyRepository.getTransferSubmissionStatusSuspended(transfer.submissionId)
                        }
                        val requestData = TransferSubmissionStatusRequestData(
                            deferred,
                            transfer
                        )
                        transferSubmissionStatusRequestList.add(requestData)
                    }
                }

                for (request in transferSubmissionStatusRequestList) {
                    val submissionStatus = request.deferred.await()

                    updateEncryptedAmount(submissionStatus, request.transfer.submissionId, request.transfer.amount.toString())

                    request.transfer.transactionStatus = submissionStatus.status
                    request.transfer.outcome = submissionStatus.outcome ?: TransactionOutcome.UNKNOWN
                    if (submissionStatus.cost != null) {
                        request.transfer.cost = submissionStatus.cost
                    }

                    //IF GTU Drop we set cost to 0
                    if(submissionStatus.status == TransactionStatus.COMMITTED && submissionStatus.sender != request.transfer.fromAddress){
                        request.transfer.cost = 0
                    }

                    Log.d("TransferSubmissionStatus Loop item end - ${request.transfer.submissionId} ${submissionStatus.status}")
                }
            } catch (e: Exception) {
                Log.e("TransferSubmissionStatus failed", e)
                handleBackendError(e)
            }
        }
        Log.d("end")
    }

    public fun updateEncryptedAmount(submissionStatus: TransferSubmissionStatus, submissionId: String, amount: String?) {
        viewModelScope.launch {
            if(submissionStatus.encryptedAmount != null){
                val amount = lookupMappedAmount(submissionId)
                saveDecryptedAmount(submissionStatus.encryptedAmount, amount) // for safe keeping as we do not have outgoing decrypted amount
            }
            else{
                saveDecryptedAmount(submissionId, amount) // for safe keeping as we do not have outgoing decrypted amount
            }
        }
    }

    private suspend fun saveAndDeleteTransfers() {
        Log.d("start")
        // Filter the list to get the ones to delete (finalized) and the ones to save
        val transfersToKeep = mutableListOf<Transfer>()
        for (transfer in transferList) {
            if (transfer.transactionStatus == TransactionStatus.FINALIZED) {
                transfersToDeleteList.add(transfer)
            } else {
                transfersToKeep.add(transfer)
            }
        }
        transferList = transfersToKeep
        transferRepository.deleteAll(transfersToDeleteList)
        transferRepository.updateAll(transferList)
        Log.d("end")
    }

    private fun loadAccountBalancesForAll() = viewModelScope.launch {
        Log.d("start")
        supervisorScope {
            try {
                for (account in accountList) {
                    if (account.transactionStatus == TransactionStatus.FINALIZED) {
                        val deferred = async {
                            proxyRepository.getAccountBalanceSuspended(account.address)
                        }
                        val requestData = AccountBalanceRequestData(deferred, account)
                        accountBalanceRequestList.add(requestData)
                    }
                }

                val accountBalanceRequestListCloned = accountBalanceRequestList.toMutableList() // prevent ConcurrentModificationException
                for (request in accountBalanceRequestListCloned) {
                    Log.d("AccountBalance Loop item start")
                    val accountBalance = request.deferred.await()
                    request.account.finalizedBalance = accountBalance.finalizedBalance?.getAmount() ?: 0
                    request.account.currentBalance = accountBalance.currentBalance?.getAmount() ?: 0

                    request.account.accountDelegation = accountBalance.currentBalance?.accountDelegation
                    request.account.accountBaker = accountBalance.currentBalance?.accountBaker

                    request.account.finalizedAccountReleaseSchedule = accountBalance.finalizedBalance?.accountReleaseSchedule
                    accountBalance.finalizedBalance?.let {

                        if(it.accountBaker != null && it.accountBaker.stakedAmount != null){
                            it.accountBaker?.stakedAmount?.toLong()?.let { request.account.totalStaked = it }
                        }
                        else{
                            request.account.totalStaked = 0
                        }

                        if(it.accountBaker != null && it.accountBaker.bakerId != null){
                            it.accountBaker?.bakerId?.toLong()?.let { request.account.bakerId = it }
                        }
                        else{
                            request.account.bakerId = null
                        }
                    }

                    if(areValuesDecrypted(request.account.finalizedEncryptedBalance)){
                        request.account.encryptedBalanceStatus = ShieldedAccountEncryptionStatus.DECRYPTED
                        request.account.finalizedEncryptedBalance = accountBalance.finalizedBalance?.getEncryptedAmount()
                        request.account.currentEncryptedBalance = accountBalance.currentBalance?.getEncryptedAmount()
                    }
                    else{
                        request.account.encryptedBalanceStatus = ShieldedAccountEncryptionStatus.ENCRYPTED
                    }

                    Log.d("AccountBalance Loop item end - ${request.account.submissionId} ${accountBalance.currentBalance}")
                }
            } catch (e: Exception) {
                Log.e("AccountBalance failed", e)
                handleBackendError(e)
            }
        }
        Log.d("end")
    }

    private suspend fun areValuesDecrypted(finalizedEncryptedBalance: AccountEncryptedAmount?): Boolean {
        if(finalizedEncryptedBalance != null){
            val selfAmountDecrypted = lookupMappedAmount(finalizedEncryptedBalance.selfAmount)
            if(selfAmountDecrypted == null){
                return false
            }
            finalizedEncryptedBalance.incomingAmounts.forEach {
                if(lookupMappedAmount(it) == null){
                    return false
                }
            }
            return true
        }
        return true
    }

    private suspend fun calculateTotalBalances(): TotalBalancesData {
        var totalBalanceForAllAccounts = 0L
        var totalBalanceForAllAccountsWithoutReadOnly = 0L
        var totalAtDisposalForSubstractionForAllAccounts = 0L
        var totalStakedForAllAccounts = 0L
        var totalContainsEncrypted = false

        for (account in accountList) {
            getTransfersForAccount(account)

            var containsEncrypted = ShieldedAccountEncryptionStatus.DECRYPTED

            var accountShieldedBalance = 0L

            //Calculate unshielded
            var accountUnshieldedBalance = account.finalizedBalance

            for (transfer in transferList) {

                if (transfer.transactionStatus != TransactionStatus.ABSENT) {

                    accountUnshieldedBalance -= transfer.cost

                    if (transfer.outcome != TransactionOutcome.Reject) {

                        //Unshielding
                        if(transfer.transactionType == TransactionType.TRANSFER){
                            accountUnshieldedBalance += transfer.amount
                            accountShieldedBalance -= transfer.amount
                        }
                        //Shielding
                        if(transfer.transactionType == TransactionType.TRANSFERTOENCRYPTED){
                            accountUnshieldedBalance -= transfer.amount
                            accountShieldedBalance += transfer.amount
                        }
                        //Plain transfer to other account
                        if(transfer.transactionType == TransactionType.TRANSFERTOPUBLIC){
                            accountUnshieldedBalance -= transfer.amount
                        }
                        //Encrypted transfer to other account
                        if(transfer.transactionType == TransactionType.ENCRYPTEDAMOUNTTRANSFER){
                            accountShieldedBalance -= transfer.amount
                        }
                    }
                }
                transfer.transactionType
            }

            //Calculate shielded
            account.finalizedEncryptedBalance?.let {
                val amount = lookupMappedAmount(it.selfAmount)
                if(amount != null){
                    accountShieldedBalance += amount.toLong()
                }
                else{
                    containsEncrypted = ShieldedAccountEncryptionStatus.ENCRYPTED
                }
                it.incomingAmounts.forEach {
                    val amount = lookupMappedAmount(it)
                    if(amount != null){
                        accountShieldedBalance += amount.toLong()
                    }
                    else{
                        if(containsEncrypted != ShieldedAccountEncryptionStatus.ENCRYPTED){
                            containsEncrypted = ShieldedAccountEncryptionStatus.PARTIALLYDECRYPTED
                        }
                    }
                }
            }

            //Calculate totals for account
            account.totalBalance = accountUnshieldedBalance + accountShieldedBalance
            account.totalUnshieldedBalance = accountUnshieldedBalance
            account.totalShieldedBalance = accountShieldedBalance
            account.encryptedBalanceStatus = containsEncrypted

            //Calculate totals for all accounts
            totalBalanceForAllAccounts += account.totalUnshieldedBalance
            if(!account.readOnly){
                totalBalanceForAllAccountsWithoutReadOnly += account.totalUnshieldedBalance
                totalAtDisposalForSubstractionForAllAccounts += account.getAtDisposalSubstraction()
                totalStakedForAllAccounts += account.totalStaked
            }

            if(containsEncrypted != ShieldedAccountEncryptionStatus.DECRYPTED){
                totalContainsEncrypted = true
            }

        }
        return TotalBalancesData(totalBalanceForAllAccounts, totalBalanceForAllAccountsWithoutReadOnly - totalAtDisposalForSubstractionForAllAccounts, totalStakedForAllAccounts, totalContainsEncrypted)
    }

    private suspend fun saveAccounts() {
        accountRepository.updateAll(accountList)
    }

    private fun handleBackendError(e: Exception) {
        if (e is CancellationException) {
            // When the coroutines are cancelled, there should not be shown an error
            return
        }
        var ex = e
        if (e is HttpException) {
            val response = e.response()
            if (response != null) {
                val error = ErrorParser.parseError(response)
                if (error != null) {
                    ex = BackendErrorException(error)
                }
            }
        }
        val stringRes = BackendErrorHandler.getExceptionStringRes(ex)
        updateListener?.onError(stringRes)
    }

    suspend fun decryptEncryptedAmounts(key: String, account: Account) {
        account.finalizedEncryptedBalance?.let {
            decryptAndSaveAmount(key,it.selfAmount)
            it.incomingAmounts.forEach {
                decryptAndSaveAmount(key, it)
            }
        }
    }

    public suspend fun decryptAndSaveAmount(key: String, encryptedAmount: String):String? {
        val output = App.appCore.cryptoLibrary.decryptEncryptedAmount(DecryptAmountInput(encryptedAmount,key))
        output?.let {
            if(lookupMappedAmount(encryptedAmount) == null){
                saveDecryptedAmount(encryptedAmount,it)
            }
        }
        return output
    }

    public suspend fun lookupMappedAmount(key: String):String? {
        if(DEFAULT_EMPTY_ENCRYPTED_AMOUNT.equals(key)){
            return 0.toString()
        }
        val result = encryptedAmountRepository.findByAddress(key)?.amount?:null

        return result
    }

    public suspend fun saveDecryptedAmount(key: String, amount: String?) {
        encryptedAmountRepository.insert(EncryptedAmount(key, amount))
    }

    suspend fun decryptAllUndecryptedAmounts(secretPrivateKey: String) {
        val list = encryptedAmountRepository.findAllUndecrypted()
        list?.forEach {
            val secretAmount = it.encryptedkey
            val output = App.appCore.cryptoLibrary.decryptEncryptedAmount(DecryptAmountInput(secretAmount,secretPrivateKey))
            output?.let {
                saveDecryptedAmount(secretAmount, output)
            }
        }
    }


}