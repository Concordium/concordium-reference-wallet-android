package com.concordium.wallet.ui.cis2

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.backend.TransactionSimulationException
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.AccountContractRepository
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateAccountTransactionInput
import com.concordium.wallet.data.cryptolib.CreateTransferInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.SerializeTokenTransferParametersInput
import com.concordium.wallet.data.cryptolib.SerializeTokenTransferParametersOutput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.SubmissionData
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.model.TransferSubmissionStatus
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.data.walletconnect.ContractAddress
import com.concordium.wallet.data.walletconnect.Payload
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toBigInteger
import com.concordium.wallet.util.toHex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Serializable
import java.math.BigInteger
import java.util.Date
import javax.crypto.Cipher

@Suppress("SerialVersionUIDInSerializableClass")
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
    private var transferSubmissionStatusRequest: BackendRequest<TransferSubmissionStatus>? = null
    private var accountBalanceRequest: BackendRequest<AccountBalance>? = null

    var sendTokenData = SendTokenData()
    val tokens: MutableLiveData<List<Token>> by lazy { MutableLiveData<List<Token>>() }
    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val transactionReady: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val feeReady: MutableLiveData<BigInteger?> by lazy { MutableLiveData<BigInteger?>() }
    val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val showAuthentication: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    var sendOnlySelectedToken: Boolean = false

    init {
        transferRepository =
            TransferRepository(WalletDatabase.getDatabase(application).transferDao())

        chooseToken.observeForever { token ->
            sendTokenData.token = token
            sendTokenData.max = if (token.isCCDToken) null else token.totalBalance
            sendTokenData.fee = null
            sendTokenData.amount = BigInteger.ZERO
            feeReady.value = null
        }
    }

    fun dispose() {
        accountNonceRequest?.dispose()
        globalParamsRequest?.dispose()
        submitTransaction?.dispose()
        accountBalanceRequest?.dispose()
        transferSubmissionStatusRequest?.dispose()
    }

    fun loadTokens(accountAddress: String) {
        waiting.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            val accountContractRepository = AccountContractRepository(
                WalletDatabase.getDatabase(getApplication()).accountContractDao()
            )
            val contractTokensRepository = ContractTokensRepository(
                WalletDatabase.getDatabase(getApplication()).contractTokenDao()
            )
            val tokensFound = mutableListOf<Token>()
            tokensFound.add(getCCDDefaultToken(accountAddress))
            sendTokenData.account?.let { account ->
                val accountContracts = accountContractRepository.find(account.address)
                accountContracts.forEach { accountContract ->
                    val tokens = contractTokensRepository.getTokens(
                        accountAddress,
                        accountContract.contractIndex
                    )
                    tokensFound.addAll(tokens.map {
                        Token(
                            it.tokenId,
                            it.tokenId,
                            "",
                            it.tokenMetadata,
                            false,
                            it.contractIndex,
                            "0",
                            false,
                            BigInteger.ZERO,
                            BigInteger.ZERO,
                            it.contractName,
                            it.tokenMetadata?.symbol ?: ""
                        )
                    })
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

    fun setMemo(memo: ByteArray?) {
        sendTokenData.memo = memo?.toHex();
        loadTransactionFee()
    }

    fun loadTransactionFee() {
        if (sendTokenData.token == null)
            return

        if (sendTokenData.token!!.isCCDToken) {
            waiting.postValue(true)
            viewModelScope.launch {
                getCCDTransferCost()
            }
            return
        }

        if (sendTokenData.account == null || sendTokenData.receiver.isEmpty()) {
            return
        }

        viewModelScope.launch {
            val serializeTokenTransferParametersInput = SerializeTokenTransferParametersInput(
                sendTokenData.token!!.token,
                sendTokenData.amount.toString(),
                sendTokenData.account!!.address,
                sendTokenData.receiver
            )
            val serializeTokenTransferParametersOutput =
                App.appCore.cryptoLibrary.serializeTokenTransferParameters(
                    serializeTokenTransferParametersInput
                )
            if (serializeTokenTransferParametersOutput == null) {
                waiting.postValue(false)
                errorInt.postValue(R.string.app_error_lib)
            } else {
                getTokenTransferCost(serializeTokenTransferParametersOutput)
            }
        }
    }

    fun hasEnoughFunds(): Boolean {
        if (sendTokenData.token == null)
            return false

        var atDisposal: BigInteger = BigInteger.ZERO
        sendTokenData.account?.let { account ->
            atDisposal =
                account.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance)
        }

        return if (sendTokenData.token!!.isCCDToken) {
            atDisposal >= sendTokenData.amount + (sendTokenData.fee ?: BigInteger.ZERO)
        } else {
            atDisposal >= (sendTokenData.fee
                ?: BigInteger.ZERO) && sendTokenData.token!!.totalBalance >= sendTokenData.amount
        }
    }

    private fun getCCDTransferCost() {
        proxyRepository.getTransferCost(
            type = ProxyRepository.SIMPLE_TRANSFER,
            memoSize = if (sendTokenData.memo == null) null else sendTokenData.memo!!.length / 2,
            success = {
                if (it.success == false) {
                    waiting.postValue(false)
                    handleBackendError(TransactionSimulationException())
                } else {
                    sendTokenData.energy = it.energy
                    sendTokenData.fee = it.cost.toBigInteger()
                    sendTokenData.account?.let { account ->
                        sendTokenData.max =
                            account.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance) -
                                    (sendTokenData.fee ?: BigInteger.ZERO)
                    }
                    waiting.postValue(false)
                    feeReady.postValue(sendTokenData.fee)
                }
            },
            failure = {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private fun getTokenTransferCost(serializeTokenTransferParametersOutput: SerializeTokenTransferParametersOutput) {
        if (sendTokenData.account == null || sendTokenData.token == null) {
            errorInt.postValue(R.string.app_error_general)
            return
        }

        proxyRepository.getTransferCost(
            type = ProxyRepository.UPDATE,
            memoSize = null,
            amount = BigInteger.ZERO,
            sender = sendTokenData.account!!.address,
            contractIndex = sendTokenData.token!!.contractIndex.toInt(),
            contractSubindex = 0,
            receiveName = sendTokenData.token!!.contractName + ".transfer",
            parameter = serializeTokenTransferParametersOutput.parameter,
            success = {
                if (it.success == false) {
                    waiting.postValue(false)
                    handleBackendError(TransactionSimulationException())
                } else {
                    sendTokenData.energy = it.energy
                    sendTokenData.fee = it.cost.toBigInteger()
                    waiting.postValue(false)
                    feeReady.postValue(sendTokenData.fee)
                }
            },
            failure = {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private suspend fun getCCDDefaultToken(accountAddress: String): Token {
        val accountRepository =
            AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
        val account = accountRepository.findByAddress(accountAddress)
        val atDisposal =
            account?.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance)
                ?: BigInteger.ZERO
        return Token(
            "",
            "CCD",
            "",
            null,
            false,
            "",
            "",
            true,
            (account?.totalBalance ?: BigInteger.ZERO),
            atDisposal,
            "",
            "CCD"
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
        val password =
            App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(cipher)
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
            val decryptedJson = App.appCore.getCurrentAuthenticationManager()
                .decryptInBackground(password, storageAccountDataEncrypted)
            val credentialsOutput =
                App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
            if (decryptedJson != null) {
                createTransaction(credentialsOutput)
            } else {
                errorInt.postValue(R.string.app_error_encryption)
                waiting.postValue(false)
            }
        }
    }

    private suspend fun createTransaction(credentialsOutput: StorageAccountData) {
        sendTokenData.expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000

        if (sendTokenData.token!!.isCCDToken)
            viewModelScope.launch {
                createCCDTransaction(
                    credentialsOutput.accountKeys
                )
            }
        else
            createTokenTransaction(credentialsOutput.accountKeys)
    }

    private suspend fun createCCDTransaction(keys: AccountData) {
        val toAddress = sendTokenData.receiver
        val nonce = sendTokenData.accountNonce
        val amount = sendTokenData.amount
        val energy = sendTokenData.energy
        val memo = sendTokenData.memo
        val expiry = sendTokenData.expiry

        if (nonce == null || energy == null || expiry == null) {
            errorInt.postValue(R.string.app_error_general)
            waiting.postValue(false)
            return
        }

        val transferInput = CreateTransferInput(
            sendTokenData.account!!.address,
            keys,
            toAddress,
            expiry,
            amount.toString(),
            energy,
            nonce.nonce,
            memo,
            null,
            null,
            null,
            null,
            null,
        )

        val output =
            App.appCore.cryptoLibrary.createTransfer(transferInput, CryptoLibrary.REGULAR_TRANSFER)

        if (output == null) {
            errorInt.postValue(R.string.app_error_general)
            waiting.postValue(false)
        } else {
            sendTokenData.createTransferOutput = output

            viewModelScope.launch {
                submitTransaction(output)
            }
        }
    }

    private fun createTokenTransaction(keys: AccountData) {
        val account = sendTokenData.account;
        val token = sendTokenData.token;
        val energy = sendTokenData.energy;
        val accountNonce = sendTokenData.accountNonce;
        val expiry = sendTokenData.expiry;

        if (account == null || token == null || energy == null || accountNonce == null || expiry == null) {
            errorInt.postValue(R.string.app_error_general)
            return
        }

        viewModelScope.launch {
            val serializeTokenTransferParametersInput = SerializeTokenTransferParametersInput(
                token.token,
                sendTokenData.amount.toString(),
                account.address,
                sendTokenData.receiver
            )
            val serializeTokenTransferParametersOutput =
                App.appCore.cryptoLibrary.serializeTokenTransferParameters(
                    serializeTokenTransferParametersInput
                )
            if (serializeTokenTransferParametersOutput == null) {
                errorInt.postValue(R.string.app_error_lib)
            } else {
                val payload = Payload.UpdateTransaction(
                    ContractAddress(token.contractIndex.toInt(), 0),
                    "0",
                    energy,
                    serializeTokenTransferParametersOutput.parameter,
                    token.contractName + ".transfer"
                )
                val accountTransactionInput = CreateAccountTransactionInput(
                    expiry.toInt(),
                    account.address,
                    keys,
                    accountNonce.nonce,
                    payload,
                    "Update"
                )
                val accountTransactionOutput =
                    App.appCore.cryptoLibrary.createAccountTransaction(accountTransactionInput)
                if (accountTransactionOutput == null) {
                    errorInt.postValue(R.string.app_error_lib)
                } else {
                    val createTransferOutput = CreateTransferOutput(
                        accountTransactionOutput.signatures,
                        "",
                        "",
                        accountTransactionOutput.transaction
                    )
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
                sendTokenData.submissionId = it.submissionId
                finishTransferCreation(it.submissionId)
                waiting.postValue(false)
            },
            {
                handleBackendError(it)
                waiting.postValue(false)
            }
        )
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }

    private fun saveNewTransfer(transfer: Transfer) = viewModelScope.launch {
        transferRepository.insert(transfer)
    }

    private fun getTransactionType(isCCDTransfer: Boolean, hasMemo: Boolean): TransactionType {
        if (!isCCDTransfer) {
            return TransactionType.UPDATE
        }
        if (hasMemo) {
            return TransactionType.TRANSFERWITHMEMO
        }
        return TransactionType.TRANSFER
    }

    private fun finishTransferCreation(submissionId: String) {
        val toAddress = sendTokenData.receiver
        val memo = sendTokenData.memo
        val expiry = sendTokenData.expiry
        val cost = sendTokenData.fee

        if (expiry == null || cost == null) {
            waiting.value = false
            return
        }
        val createdAt = Date().time
        var newStartIndex: Int = 0
        val isCCDTransfer = sendTokenData.token!!.isCCDToken

        val amount = if (isCCDTransfer) sendTokenData.amount else BigInteger.ZERO

        val transfer = Transfer(
            0,
            sendTokenData.account?.id ?: -1,
            amount,
            cost,
            sendTokenData.account?.address.orEmpty(),
            toAddress,
            expiry,
            memo,
            createdAt,
            submissionId,
            TransactionStatus.UNKNOWN,
            TransactionOutcome.UNKNOWN,
            getTransactionType(isCCDTransfer, memo != null),
            null,
            newStartIndex,
            sendTokenData.accountNonce
        )
        waiting.postValue(false)
        saveNewTransfer(transfer)
        transactionReady.postValue(submissionId)
    }
}
