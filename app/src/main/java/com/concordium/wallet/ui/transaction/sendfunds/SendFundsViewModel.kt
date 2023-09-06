package com.concordium.wallet.ui.transaction.sendfunds

import android.app.Application
import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.CBORUtil
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.backend.BackendErrorException
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateTransferInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.InputEncryptedAmount
import com.concordium.wallet.data.model.SubmissionData
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.model.TransferSubmissionStatus
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.preferences.SharedPreferencesKeys
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toBigInteger
import com.concordium.wallet.util.toHex
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.Date
import javax.crypto.Cipher

class SendFundsViewModel(application: Application) : AndroidViewModel(application) {

    private var sendAll: Boolean = false
    private val proxyRepository = ProxyRepository()
    private val accountRepository: AccountRepository
    private val transferRepository: TransferRepository

    companion object {
        val KEY_SHOW_MEMO_WARNING = "KEY_SHOW_MEMO_WARNING_V2"
    }

    class SendFundsPreferences(context: Context, preferenceName: String, preferenceMode: Int) :
        Preferences(context, preferenceName, preferenceMode) {
        fun showMemoWarning(): Boolean {
            return getBoolean(KEY_SHOW_MEMO_WARNING, true)
        }

        fun dontShowMemoWarning() {
            setBoolean(KEY_SHOW_MEMO_WARNING, false)
        }
    }

    private val preferences: SendFundsPreferences
        get() {
            return SendFundsPreferences(
                getApplication(),
                SharedPreferencesKeys.PREF_SEND_FUNDS.key,
                Context.MODE_PRIVATE
            )
        }

    private val gson = App.appCore.gson

    private val accountUpdater = AccountUpdater(application, viewModelScope)

    private var accountNonceRequest: BackendRequest<AccountNonce>? = null
    private var submitCredentialRequest: BackendRequest<SubmissionData>? = null
    private var transferSubmissionStatusRequest: BackendRequest<TransferSubmissionStatus>? = null
    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null
    private var accountBalanceRequest: BackendRequest<AccountBalance>? = null

    lateinit var account: Account
    var isShielded: Boolean = false

    var selectedRecipient: Recipient? = null
        set(value) {
            field = value
            _recipientLiveData.value = value
            loadTransactionFee()
            if (value != null && isShielded && !isTransferToSameAccount()) {
                getAccountEncryptedKey(value.address)
            }
        }
    private var tempData = TempData()
    var newTransfer: Transfer? = null

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    private val _gotoSendFundsConfirmLiveData = MutableLiveData<Event<Boolean>>()
    val gotoSendFundsConfirmLiveData: LiveData<Event<Boolean>>
        get() = _gotoSendFundsConfirmLiveData

    private val _gotoFailedLiveData = MutableLiveData<Event<Pair<Boolean, BackendError?>>>()
    val gotoFailedLiveData: LiveData<Event<Pair<Boolean, BackendError?>>>
        get() = _gotoFailedLiveData

    private val _transactionFeeLiveData = MutableLiveData<BigInteger>()
    val transactionFeeLiveData: LiveData<BigInteger>
        get() = _transactionFeeLiveData

    private val _waitingReceiverAccountPublicKeyLiveData = MutableLiveData<Boolean>()
    val waitingReceiverAccountPublicKeyLiveData: LiveData<Boolean>
        get() = _waitingReceiverAccountPublicKeyLiveData

    private val _recipientLiveData = MutableLiveData<Recipient?>()
    val recipientLiveData: MutableLiveData<Recipient?>
        get() = _recipientLiveData

    private val _sendAllAmountLiveData = MutableLiveData<BigInteger>()
    val sendAllAmountLiveData: LiveData<BigInteger>
        get() = _sendAllAmountLiveData

    private class TempData {
        var accountNonce: AccountNonce? = null
        var toAddress: String? = null
        var amount: BigInteger? = null
        var energy: BigInteger? = null
        var submissionId: String? = null
        var transferSubmissionStatus: TransferSubmissionStatus? = null
        var expiry: Long? = null
        var memo: String? = null
        var globalParams: GlobalParams? = null
        var receiverPublicKey: String? = null
        var accountBalance: AccountBalance? = null
        var createTransferInput: CreateTransferInput? = null
        var createTransferOutput: CreateTransferOutput? = null
        var newSelfEncryptedamount: String? = null
    }

    init {
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
        val transferDao = WalletDatabase.getDatabase(application).transferDao()
        transferRepository = TransferRepository(transferDao)
    }

    fun initialize(account: Account, isShielded: Boolean) {
        this.account = account
        this.isShielded = isShielded
        getGlobalInfo()
        getAccountBalance()
    }

    override fun onCleared() {
        super.onCleared()
        accountNonceRequest?.dispose()
        submitCredentialRequest?.dispose()
        transferSubmissionStatusRequest?.dispose()
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        if (throwable is BackendErrorException) {
            _gotoFailedLiveData.value = Event(Pair(true, throwable.error))
        } else {
            _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(throwable))
        }
    }

    private fun loadTransactionFee() {
        val type =
            if (isShielded) {
                if (isTransferToSameAccount()) {
                    ProxyRepository.TRANSFER_TO_PUBLIC
                } else {
                    ProxyRepository.ENCRYPTED_TRANSFER
                }
            } else {
                if (isTransferToSameAccount()) {
                    ProxyRepository.TRANSFER_TO_SECRET
                } else {
                    ProxyRepository.SIMPLE_TRANSFER
                }
            }

        proxyRepository.getTransferCost(type = type,
            memoSize = if (tempData.memo == null) null else tempData.memo!!.length / 2, //div by 2 because hex takes up twice the length
            success = {
                tempData.energy = it.energy
                _transactionFeeLiveData.value = it.cost.toBigInteger()
                updateSendAllAmount()
            },
            failure = {
                handleBackendError(it)
            }
        )
    }

    fun isTransferToSameAccount(): Boolean {
        return account.address == selectedRecipient?.address
    }

    fun getAmount(): BigInteger? {
        return tempData.amount
    }

    fun hasSufficientFunds(amount: String): Boolean {
        val amountValue = CurrencyUtil.toGTUValue(amount)
        val cost = _transactionFeeLiveData.value
        if (amountValue == null || cost == null) {
            return true
        }

        val totalUnshieldedAtDisposal =
            account.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance)

        if (isShielded) {
            if (isTransferToSameAccount()) {
                //SEC_TO_PUBLIC_TRANSFER
                if (amountValue > account.totalShieldedBalance || cost > totalUnshieldedAtDisposal) {
                    return false
                }
            } else {
                //ENCRYPTED_TRANSFER
                if (amountValue > account.totalShieldedBalance || cost > totalUnshieldedAtDisposal) {
                    return false
                }
            }
        } else {
            if (isTransferToSameAccount()) {
                //PUBLIC_TO_SEC_TRANSFER
                if (amountValue + cost > totalUnshieldedAtDisposal) {
                    return false
                }
            } else {
                //REGULAR_TRANSFER
                if (amountValue + cost > totalUnshieldedAtDisposal) {
                    return false
                }
            }
        }
        return true
    }

    fun sendFunds(amount: String) {
        val amountValue = CurrencyUtil.toGTUValue(amount)
        if (amountValue == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            return
        }
        tempData.amount = amountValue

        val recipient = selectedRecipient
        if (recipient == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            return
        }
        tempData.toAddress = recipient.address
        getAccountNonce()
    }

    private fun getAccountNonce() {
        _waitingLiveData.value = true
        accountNonceRequest?.dispose()
        accountNonceRequest = proxyRepository.getAccountNonce(account.address,
            {
                tempData.accountNonce = it
                _showAuthenticationLiveData.value = Event(true)
                _waitingLiveData.value = false
            },
            {
                _waitingLiveData.value = false
                handleBackendError(it)
            }
        )
    }

    fun getCipherForBiometrics(): Cipher? {
        return try {
            val cipher =
                App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForDecryption()
            if (cipher == null) {
                _errorLiveData.value = Event(R.string.app_error_keystore_key_invalidated)
            }
            cipher
        } catch (e: KeystoreEncryptionException) {
            _errorLiveData.value = Event(R.string.app_error_keystore)
            null
        }
    }

    fun continueWithPassword(password: String) = viewModelScope.launch {
        _waitingLiveData.value = true
        decryptAndContinue(password)
    }

    fun checkLogin(cipher: Cipher) = viewModelScope.launch {
        _waitingLiveData.value = true
        val password =
            App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(cipher)
        if (password != null) {
            decryptAndContinue(password)
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
        }
    }

    private suspend fun decryptAndContinue(password: String) {
        // Decrypt the private data
        val storageAccountDataEncrypted = account.encryptedAccountData
        if (TextUtils.isEmpty(storageAccountDataEncrypted)) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return
        }
        val decryptedJson = App.appCore.getCurrentAuthenticationManager()
            .decryptInBackground(password, storageAccountDataEncrypted)

        if (decryptedJson != null) {
            val credentialsOutput = gson.fromJson(decryptedJson, StorageAccountData::class.java)
            createTransfer(credentialsOutput.accountKeys, credentialsOutput.encryptionSecretKey)
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
        }
    }

    private suspend fun createTransfer(
        keys: AccountData,
        encryptionSecretKey: String
    ) {
        val toAddress = tempData.toAddress
        val nonce = tempData.accountNonce
        val amount = tempData.amount
        val energy = tempData.energy
        val memo = tempData.memo

        if (toAddress == null || nonce == null || amount == null || energy == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return
        }

        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000
        tempData.expiry = expiry
        val transferInput = CreateTransferInput(
            account.address,
            keys,
            toAddress,
            expiry,
            amount.toString(),
            energy.toInt(),
            nonce.nonce,
            memo,
            tempData.globalParams,
            tempData.receiverPublicKey,
            encryptionSecretKey,
            calculateInputEncryptedAmount(),
            null,
            null,
            null
        )

        var transactionType =
            if (isShielded) {
                if (isTransferToSameAccount()) {
                    CryptoLibrary.SEC_TO_PUBLIC_TRANSFER
                } else {
                    CryptoLibrary.ENCRYPTED_TRANSFER
                }
            } else {
                if (isTransferToSameAccount()) {
                    CryptoLibrary.PUBLIC_TO_SEC_TRANSFER
                } else {
                    CryptoLibrary.REGULAR_TRANSFER
                }
            }
        tempData.createTransferInput = transferInput

        val output = App.appCore.cryptoLibrary.createTransfer(transferInput, transactionType)
        if (output == null) {
            _errorLiveData.value = Event(R.string.app_error_lib)
            _waitingLiveData.value = false
        } else {
            tempData.createTransferOutput = output

            //Save and update new selfAmount for later lookup
            viewModelScope.launch {
                if (output.addedSelfEncryptedAmount != null) {
                    account.finalizedEncryptedBalance?.let { encBalance ->
                        val newEncryptedAmount = App.appCore.cryptoLibrary.combineEncryptedAmounts(
                            output.addedSelfEncryptedAmount,
                            encBalance.selfAmount
                        ).toString()
                        tempData.newSelfEncryptedamount = newEncryptedAmount
                        val oldDecryptedAmount =
                            accountUpdater.lookupMappedAmount(encBalance.selfAmount)
                        oldDecryptedAmount?.let {
                            accountUpdater.saveDecryptedAmount(
                                newEncryptedAmount,
                                (it.toBigInteger() + amount).toString()
                            )
                        }
                    }
                }
                if (output.remaining != null) {
                    tempData.newSelfEncryptedamount = output.remaining
                    val remainingAmount =
                        accountUpdater.decryptAndSaveAmount(encryptionSecretKey, output.remaining)

                    account.finalizedEncryptedBalance?.let { encBalance ->
                        val oldDecryptedAmount =
                            accountUpdater.lookupMappedAmount(encBalance.selfAmount)
                        oldDecryptedAmount?.let {
                            accountUpdater.saveDecryptedAmount(
                                output.remaining,
                                remainingAmount.toString()
                            )
                        }
                    }
                }
                submitTransfer(output)
            }

        }
    }

    private suspend fun calculateInputEncryptedAmount(): InputEncryptedAmount? {

        val lastNounceToInclude = tempData.accountBalance?.finalizedBalance?.accountNonce ?: -2

        val allTransfers = transferRepository.getAllByAccountId(account.id)
        val unfinalisedTransfers = allTransfers.filter {
            it.transactionStatus != TransactionStatus.FINALIZED && (it.nonce?.nonce
                ?: -1) >= lastNounceToInclude
        }

        val aggEncryptedAmount = if (unfinalisedTransfers.isNotEmpty()) {
            val lastTransaction =
                unfinalisedTransfers.maxWithOrNull { a, b -> a.id.compareTo(b.id) }
            if (lastTransaction != null) {
                tempData.accountBalance?.finalizedBalance?.let {
                    val incomingAmounts = it.accountEncryptedAmount.incomingAmounts.filter {
                        accountUpdater.lookupMappedAmount(it) != null
                    }
                    var agg = lastTransaction.newSelfEncryptedAmount ?: ""
                    for (i in lastTransaction.newStartIndex until it.accountEncryptedAmount.startIndex + incomingAmounts.count()) {
                        agg = App.appCore.cryptoLibrary.combineEncryptedAmounts(
                            agg,
                            incomingAmounts[i]
                        ).toString()
                    }
                    agg
                } ?: ""
            } else {
                ""
            }
        } else {
            tempData.accountBalance?.finalizedBalance?.let {
                var agg = it.accountEncryptedAmount.selfAmount
                it.accountEncryptedAmount.incomingAmounts.forEach {
                    if (accountUpdater.lookupMappedAmount(it) != null) {
                        agg = App.appCore.cryptoLibrary.combineEncryptedAmounts(agg, it).toString()
                    }
                }
                agg
            } ?: ""
        }

        val aggAmount = tempData.accountBalance?.finalizedBalance?.let {
            var agg =
                accountUpdater.lookupMappedAmount(it.accountEncryptedAmount.selfAmount)
                    ?.toBigInteger()
                    ?: BigInteger.ZERO
            it.accountEncryptedAmount.incomingAmounts.forEach {
                agg += accountUpdater.lookupMappedAmount(it)?.toBigInteger() ?: BigInteger.ZERO
            }
            unfinalisedTransfers.forEach {
                agg -= it.amount
            }
            agg
        } ?: ""

        val index = tempData.accountBalance?.finalizedBalance?.let {
            it.accountEncryptedAmount.startIndex + it.accountEncryptedAmount.incomingAmounts.count {
                accountUpdater.lookupMappedAmount(it) != null
            }
        } ?: 0

        return InputEncryptedAmount(aggEncryptedAmount, aggAmount.toString(), index)
    }

    private fun getGlobalInfo() {
        // Show waiting state for the full flow, but remove it of any errors occur
        _waitingLiveData.value = true
        globalParamsRequest?.dispose()
        globalParamsRequest = proxyRepository.getIGlobalInfo(
            {
                tempData.globalParams = it.value
                _waitingLiveData.value = false
            },
            {
                _waitingLiveData.value = false
                handleBackendError(it)
            }
        )
    }

    private fun getAccountEncryptedKey(accountAddress: String) {
        _waitingReceiverAccountPublicKeyLiveData.value = true
        proxyRepository.getAccountEncryptedKey(
            accountAddress.trim(),
            {
                tempData.receiverPublicKey = it.accountEncryptionKey
                _waitingReceiverAccountPublicKeyLiveData.value = false
            },
            {
                _waitingReceiverAccountPublicKeyLiveData.value = false
            }
        )
    }

    private fun getAccountBalance() {
        _waitingLiveData.value = true
        accountBalanceRequest?.dispose()
        accountBalanceRequest = proxyRepository.getAccountBalance(account.address,
            {
                tempData.accountBalance = it
                _waitingLiveData.value = false
            },
            {
                _waitingLiveData.value = false
                handleBackendError(it)
            }
        )
    }

    private fun submitTransfer(transfer: CreateTransferOutput) {
        _waitingLiveData.value = true
        submitCredentialRequest?.dispose()
        submitCredentialRequest = proxyRepository.submitTransfer(transfer,
            {
                tempData.submissionId = it.submissionId
                submissionStatus(it.submissionId)
                // Do not disable waiting state yet
            },
            {
                _waitingLiveData.value = false
                handleBackendError(it)
            }
        )
    }

    private fun submissionStatus(submissionId: String) {
        _waitingLiveData.value = true
        transferSubmissionStatusRequest?.dispose()
        transferSubmissionStatusRequest = proxyRepository.getTransferSubmissionStatus(submissionId,
            {
                tempData.transferSubmissionStatus = it
                accountUpdater.updateEncryptedAmount(it, submissionId, tempData.amount?.toString())
                finishTransferCreation()
                // Do not disable waiting state yet
            },
            {
                _waitingLiveData.value = false
                _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
            }
        )
    }

    private fun finishTransferCreation() {
        val amount = tempData.amount
        val toAddress = tempData.toAddress
        val memo = tempData.memo
        val submissionId = tempData.submissionId
        val transferSubmissionStatus = tempData.transferSubmissionStatus
        val expiry = tempData.expiry
        val cost = transactionFeeLiveData.value

        if (amount == null || toAddress == null || submissionId == null ||
            transferSubmissionStatus == null || expiry == null || cost == null
        ) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return
        }
        val createdAt = Date().time
        var newStartIndex: Int = 0
        tempData.createTransferInput?.let {
            newStartIndex = it.inputEncryptedAmount?.aggIndex ?: 0
        }

        val transfer = Transfer(
            0,
            account.id,
            amount,
            cost,
            account.address,
            toAddress,
            expiry,
            memo,
            createdAt,
            submissionId,
            transferSubmissionStatus.status,
            transferSubmissionStatus.outcome ?: TransactionOutcome.UNKNOWN,
            if (isShielded) {
                if (isTransferToSameAccount()) {
                    if (memo == null || memo.isEmpty()) {
                        TransactionType.TRANSFER
                    } else {
                        TransactionType.TRANSFERWITHMEMO
                    }
                } else {
                    if (memo == null || memo.isEmpty()) {
                        TransactionType.ENCRYPTEDAMOUNTTRANSFER
                    } else {
                        TransactionType.ENCRYPTEDAMOUNTTRANSFERWITHMEMO
                    }
                }
            } else {
                if (isTransferToSameAccount()) {
                    TransactionType.TRANSFERTOENCRYPTED
                } else {
                    TransactionType.TRANSFERTOPUBLIC
                }
            },
            tempData.newSelfEncryptedamount,
            newStartIndex,
            tempData.accountNonce
        )
        newTransfer = transfer
        saveNewTransfer(transfer)
    }

    private fun saveNewTransfer(transfer: Transfer) = viewModelScope.launch {
        transferRepository.insert(transfer)

        _gotoSendFundsConfirmLiveData.value = Event(true)
    }

    fun setMemo(memo: ByteArray?) {
        if (memo != null) {
            tempData.memo = memo.toHex()
        } else {
            tempData.memo = null
        }
        loadTransactionFee()
    }

    fun usePasscode(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().usePasscode()
    }

    fun showMemoWarning(): Boolean {
        return preferences.showMemoWarning()
    }

    fun dontShowMemoWarning() {
        return preferences.dontShowMemoWarning()
    }

    fun getClearTextMemo(): String? {
        if (tempData.memo == null) {
            return null
        } else {
            return CBORUtil.decodeHexAndCBOR(tempData.memo!!)
        }
    }

    fun updateSendAllValue() {
        sendAll = true
        loadTransactionFee()
    }

    fun disableSendAllValue() {
        sendAll = false
    }

    fun updateSendAllAmount() {
        if (sendAll) {
            var cost = BigInteger.ZERO
            _transactionFeeLiveData.value?.let {
                cost = it
            }
            var amount: BigInteger =
                ((if (isShielded) account.totalShieldedBalance else (account.getAtDisposalWithoutStakedOrScheduled(
                    account.totalUnshieldedBalance
                ) - cost)))
            if (amount.signum() < 0) {
                amount = BigInteger.ZERO
            }
            _sendAllAmountLiveData.value = amount
        }
    }

    fun validateAndSaveRecipient(address: String): Boolean {
        val isAddressValid = App.appCore.cryptoLibrary.checkAccountAddress(address)
        if (!isAddressValid) {
            _errorLiveData.value = Event(R.string.recipient_error_invalid_address)
            return false
        }
        return true
    }
}
