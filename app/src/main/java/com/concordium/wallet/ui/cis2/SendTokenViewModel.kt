package com.concordium.wallet.ui.cis2

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.AccountContractRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.*
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.data.walletconnect.ContractAddress
import com.concordium.wallet.data.walletconnect.Payload
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Serializable
import javax.crypto.Cipher

data class SendTokenData(
    var token: Token? = null,
    var account: Account? = null,
    var amount: Long = 0,
    var receiver: String = "",
    var fee: Long? = null,
    var max: Long? = null,
    var memo: String? = null,
    var energy: Long? = null,
    var accountNonce: AccountNonce? = null,
    var expiry: Long? = null,
    var createTransferInput: CreateTransferInput? = null,
    var createTransferOutput: CreateTransferOutput? = null,
    var receiverPublicKey: String? = null,
    var globalParams: GlobalParams? = null,
    var accountBalance: AccountBalance? = null,
    var newSelfEncryptedAmount: String? = null
): Serializable

class SendTokenViewModel(application: Application) : AndroidViewModel(application), Serializable {
    companion object {
        const val SEND_TOKEN_DATA = "SEND_TOKEN_DATA"
    }

    private val proxyRepository = ProxyRepository()
    private val transferRepository: TransferRepository
    private val accountUpdater = AccountUpdater(application, viewModelScope)

    private var accountNonceRequest: BackendRequest<AccountNonce>? = null
    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null
    private var submitTransaction: BackendRequest<SubmissionData>? = null
    private var accountBalanceRequest: BackendRequest<AccountBalance>? = null

    var sendTokenData = SendTokenData()
    val tokens: MutableLiveData<List<Token>> by lazy { MutableLiveData<List<Token>>() }
    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val transactionReady: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val feeReady: MutableLiveData<Long> by lazy { MutableLiveData<Long>() }
    val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val showAuthentication: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    init {
        transferRepository = TransferRepository(WalletDatabase.getDatabase(application).transferDao())
    }

    fun dispose() {
        accountNonceRequest?.dispose()
        globalParamsRequest?.dispose()
        submitTransaction?.dispose()
        accountBalanceRequest?.dispose()
    }

    fun loadTokens() {
        waiting.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            val accountContractRepository = AccountContractRepository(WalletDatabase.getDatabase(getApplication()).accountContractDao())
            val contractTokensRepository = ContractTokensRepository(WalletDatabase.getDatabase(getApplication()).contractTokenDao())
            val tokensFound = mutableListOf<Token>()
            sendTokenData.account?.let { account ->
                val accountContracts = accountContractRepository.find(account.address)
                accountContracts.forEach { accountContract ->
                    val tokens = contractTokensRepository.getTokens(accountContract.contractIndex)
                    tokensFound.addAll(tokens.map { Token(it.tokenId, "", "", null, false, "", false, 0, 0, "", "CCD") })
                }
            }
            waiting.postValue(false)
            tokens.postValue(tokensFound)
        }
    }

    fun getGlobalInfo() {
        waiting.postValue(true)
        globalParamsRequest?.dispose()
        globalParamsRequest = proxyRepository.getIGlobalInfo(
            {
                sendTokenData.globalParams = it.value
                waiting.postValue(false)
            },
            {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    fun getAccountBalance() {
        if (sendTokenData.account == null)
            return

        waiting.postValue(true)
        accountBalanceRequest?.dispose()
        accountBalanceRequest = proxyRepository.getAccountBalance(sendTokenData.account!!.address,
            {
                sendTokenData.accountBalance = it
                waiting.postValue(false)
            },
            {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    fun send() {
        waiting.postValue(true)
        accountNonceRequest?.dispose()
        accountNonceRequest = proxyRepository.getAccountNonce(sendTokenData.account?.address ?: "",
            {
                waiting.postValue(false)
                sendTokenData.accountNonce = it
                showAuthentication.postValue(true)
            },
            {
                waiting.postValue(false)
                handleBackendError(it)
            })
    }

    fun loadTransactionFee() {
        if (sendTokenData.token == null)
            return

        if (sendTokenData.token!!.isCCDToken) {
            waiting.postValue(true)
            viewModelScope.launch {
                getTransferCostCCD()
            }
            return
        }

        if (sendTokenData.account == null || sendTokenData.receiver.isEmpty()) {
            return
        }

        viewModelScope.launch {
            val serializeTokenTransferParametersInput = SerializeTokenTransferParametersInput(sendTokenData.token!!.token, sendTokenData.amount.toString(), sendTokenData.account!!.address, sendTokenData.receiver)
            val serializeTokenTransferParametersOutput = App.appCore.cryptoLibrary.serializeTokenTransferParameters(serializeTokenTransferParametersInput)
            if (serializeTokenTransferParametersOutput == null) {
                waiting.postValue(false)
                errorInt.postValue(R.string.app_error_lib)
            } else {
                getTransferCost(serializeTokenTransferParametersOutput)
            }
        }
    }

    private fun getTransferCostCCD() {
        proxyRepository.getTransferCost(
            type = ProxyRepository.SIMPLE_TRANSFER,
            memoSize = if (sendTokenData.memo == null) null else sendTokenData.memo!!.length / 2,
            success = {
                sendTokenData.energy = it.energy
                sendTokenData.fee = it.cost.toLong()
                sendTokenData.account?.let { account ->
                    sendTokenData.max = account.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance) - (sendTokenData.fee ?: 0)
                }
                waiting.postValue(false)
                feeReady.postValue(sendTokenData.fee)
            },
            failure = {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private fun getTransferCost(serializeTokenTransferParametersOutput: SerializeTokenTransferParametersOutput) {
        if (sendTokenData.account == null || sendTokenData.token == null) {
            errorInt.postValue(R.string.app_error_general)
            return
        }

        proxyRepository.getTransferCost(
            type = ProxyRepository.UPDATE,
            memoSize = null,
            amount = sendTokenData.amount,
            sender = sendTokenData.account!!.address,
            contractIndex = sendTokenData.token!!.contractIndex.toInt(),
            contractSubindex = 0,
            receiveName = sendTokenData.token!!.contractName + ".transfer",
            parameter = serializeTokenTransferParametersOutput.parameter,
            success = {
                sendTokenData.energy = it.energy
                sendTokenData.fee = it.cost.toLong()
                sendTokenData.account?.let { account ->
                    sendTokenData.max = account.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance) - (sendTokenData.fee ?: 0)
                }
                waiting.postValue(false)
                feeReady.postValue(sendTokenData.fee)
            },
            failure = {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    fun getCipherForBiometrics(): Cipher? {
        return try {
            val cipher =
                App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForDecryption()
            if (cipher == null) {
                errorInt.postValue(R.string.app_error_keystore_key_invalidated)
            }
            cipher
        } catch (e: KeystoreEncryptionException) {
            errorInt.postValue(R.string.app_error_keystore)
            null
        }
    }

    fun continueWithPassword(password: String) = viewModelScope.launch {
        waiting.postValue(true)
        decryptAndContinue(password)
    }

    fun checkLogin(cipher: Cipher) = viewModelScope.launch {
        waiting.postValue(true)
        val password = App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(cipher)
        if (password != null) {
            decryptAndContinue(password)
        } else {
            errorInt.postValue(R.string.app_error_encryption)
            waiting.postValue(false)
        }
    }

    private suspend fun decryptAndContinue(password: String) {
        sendTokenData.account?.let { account ->
            val storageAccountDataEncrypted = account.encryptedAccountData
            if (TextUtils.isEmpty(storageAccountDataEncrypted)) {
                errorInt.postValue(R.string.app_error_general)
                waiting.postValue(false)
                return
            }
            val decryptedJson = App.appCore.getCurrentAuthenticationManager().decryptInBackground(password, storageAccountDataEncrypted)
            val credentialsOutput = App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
            if (decryptedJson != null) {
                getAccountEncryptedKey(credentialsOutput)
            } else {
                errorInt.postValue(R.string.app_error_encryption)
                waiting.postValue(false)
            }
        }
    }

    private suspend fun getAccountEncryptedKey(credentialsOutput: StorageAccountData) {
        proxyRepository.getAccountEncryptedKey(
            sendTokenData.receiver,
            {
                sendTokenData.receiverPublicKey = it.accountEncryptionKey
                if (sendTokenData.token!!.isCCDToken)
                    viewModelScope.launch {
                        createTransactionCCD(credentialsOutput.accountKeys, credentialsOutput.encryptionSecretKey)
                    }
                else
                    createTransaction(credentialsOutput.accountKeys, credentialsOutput.encryptionSecretKey)
            },
            {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private suspend fun createTransactionCCD(keys: AccountData, encryptionSecretKey: String) {
        val toAddress = sendTokenData.receiver
        val nonce = sendTokenData.accountNonce
        val amount = sendTokenData.amount
        val energy = sendTokenData.energy
        val memo = sendTokenData.memo

        if (nonce == null || energy == null) {
            errorInt.postValue(R.string.app_error_general)
            waiting.postValue(false)
            return
        }

        sendTokenData.expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000

        val transferInput = CreateTransferInput(
            sendTokenData.account!!.address,
            keys,
            toAddress,
            sendTokenData.expiry!!,
            amount.toString(),
            energy,
            nonce.nonce,
            memo,
            sendTokenData.globalParams,
            sendTokenData.receiverPublicKey,
            encryptionSecretKey,
            calculateInputEncryptedAmount(),
            null,
            null,
            null)

        sendTokenData.createTransferInput = transferInput

        val output = App.appCore.cryptoLibrary.createTransfer(transferInput, CryptoLibrary.REGULAR_TRANSFER)

        if (output == null) {
            errorInt.postValue(R.string.app_error_general)
            waiting.postValue(false)
        } else {
            sendTokenData.createTransferOutput = output

            viewModelScope.launch {
                if (output.addedSelfEncryptedAmount != null) {
                    sendTokenData.account!!.finalizedEncryptedBalance?.let { encBalance ->
                        val newEncryptedAmount = App.appCore.cryptoLibrary.combineEncryptedAmounts(
                            output.addedSelfEncryptedAmount,
                            encBalance.selfAmount
                        ).toString()
                        sendTokenData.newSelfEncryptedAmount = newEncryptedAmount
                        val oldDecryptedAmount =
                            accountUpdater.lookupMappedAmount(encBalance.selfAmount)
                        oldDecryptedAmount?.let {
                            accountUpdater.saveDecryptedAmount(
                                newEncryptedAmount,
                                (it.toLong() + amount).toString()
                            )
                        }
                    }
                }
                if (output.remaining != null) {
                    sendTokenData.newSelfEncryptedAmount = output.remaining
                    val remainingAmount =
                        accountUpdater.decryptAndSaveAmount(encryptionSecretKey, output.remaining)

                    sendTokenData.account!!.finalizedEncryptedBalance?.let { encBalance ->
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
                submitTransaction(output)
            }
        }
    }

    private fun createTransaction(keys: AccountData, encryptionSecretKey: String) {
        if (sendTokenData.account == null || sendTokenData.token == null || sendTokenData.energy == null || sendTokenData.accountNonce == null) {
            errorInt.postValue(R.string.app_error_general)
            return
        }

        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000

        viewModelScope.launch {
            val serializeTokenTransferParametersInput = SerializeTokenTransferParametersInput(sendTokenData.token!!.token, sendTokenData.amount.toString(), sendTokenData.account!!.address, sendTokenData.receiver)
            val serializeTokenTransferParametersOutput = App.appCore.cryptoLibrary.serializeTokenTransferParameters(serializeTokenTransferParametersInput)
            if (serializeTokenTransferParametersOutput == null) {
                errorInt.postValue(R.string.app_error_lib)
            } else {
                val payload = Payload(ContractAddress(sendTokenData.token!!.contractIndex.toInt(), 0), sendTokenData.amount.toString(), sendTokenData.energy!!.toInt(), serializeTokenTransferParametersOutput.parameter, sendTokenData.token!!.contractName + ".transfer")
                val accountTransactionInput = CreateAccountTransactionInput(expiry.toInt(), sendTokenData.account!!.address, keys, sendTokenData.accountNonce!!.nonce, payload, "Update")
                val accountTransactionOutput = App.appCore.cryptoLibrary.createAccountTransaction(accountTransactionInput)
                if (accountTransactionOutput == null) {
                    errorInt.postValue(R.string.app_error_lib)
                } else {
                    val createTransferOutput = CreateTransferOutput(accountTransactionOutput.signatures, "", "", accountTransactionOutput.transaction)
                    submitTransaction(createTransferOutput)
                }
            }
        }
    }

    private fun submitTransaction(createTransferOutput: CreateTransferOutput) {
        waiting.postValue(true)
        submitTransaction?.dispose()
        submitTransaction = proxyRepository.submitTransfer(createTransferOutput,
            {
                println("LC -> submitTransaction SUCCESS = ${it.submissionId}")
                waiting.postValue(false)
                transactionReady.postValue(true)
            },
            {
                println("LC -> submitTransaction ERROR ${it.stackTraceToString()}")
                handleBackendError(it)
                waiting.postValue(false)
            }
        )
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }

    private suspend fun calculateInputEncryptedAmount(): InputEncryptedAmount? {
        if (sendTokenData.account == null)
            return null

        val lastNounceToInclude = sendTokenData.accountBalance?.finalizedBalance?.accountNonce ?: -2

        val allTransfers = transferRepository.getAllByAccountId(sendTokenData.account!!.id)
        val unfinalisedTransfers = allTransfers.filter {
            it.transactionStatus != TransactionStatus.FINALIZED && (it.nonce?.nonce ?: -1) >= lastNounceToInclude
        }

        val aggEncryptedAmount = if (unfinalisedTransfers.isNotEmpty()) {
            val lastTransaction = unfinalisedTransfers.maxWithOrNull { a, b -> a.id.compareTo(b.id) }
            if (lastTransaction != null) {
                sendTokenData.accountBalance?.finalizedBalance?.let { accountBalanceInfo ->
                    val incomingAmounts = accountBalanceInfo.accountEncryptedAmount.incomingAmounts.filter { incomingAmount ->
                        accountUpdater.lookupMappedAmount(incomingAmount) != null
                    }
                    var agg = lastTransaction.newSelfEncryptedAmount ?: ""
                    for (i in lastTransaction.newStartIndex until accountBalanceInfo.accountEncryptedAmount.startIndex + incomingAmounts.count()) {
                        agg = App.appCore.cryptoLibrary.combineEncryptedAmounts(agg, incomingAmounts[i]).toString()
                    }
                    agg
                } ?: ""
            } else {
                ""
            }
        } else {
            sendTokenData.accountBalance?.finalizedBalance?.let {
                var agg = it.accountEncryptedAmount.selfAmount
                it.accountEncryptedAmount.incomingAmounts.forEach { incomingAmount ->
                    if (accountUpdater.lookupMappedAmount(incomingAmount) != null) {
                        agg = App.appCore.cryptoLibrary.combineEncryptedAmounts(agg, incomingAmount).toString()
                    }
                }
                agg
            } ?: ""
        }

        val aggAmount = sendTokenData.accountBalance?.finalizedBalance?.let { accountBalanceInfo ->
            var agg = accountUpdater.lookupMappedAmount(accountBalanceInfo.accountEncryptedAmount.selfAmount)?.toLong() ?: 0
            accountBalanceInfo.accountEncryptedAmount.incomingAmounts.forEach { incomingAmount ->
                agg += accountUpdater.lookupMappedAmount(incomingAmount)?.toLong() ?: 0
            }
            unfinalisedTransfers.forEach { transfer ->
                agg -= transfer.amount
            }
            agg
        } ?: ""

        val index = sendTokenData.accountBalance?.finalizedBalance?.let {
            it.accountEncryptedAmount.startIndex + it.accountEncryptedAmount.incomingAmounts.count {
                accountUpdater.lookupMappedAmount(it) != null
            }
        } ?: 0

        return InputEncryptedAmount(aggEncryptedAmount, aggAmount.toString(), index)
    }
}
