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
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.*
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.WalletDatabase
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
    var globalParams: GlobalParams? = null
): Serializable

class SendTokenViewModel(application: Application) : AndroidViewModel(application), Serializable {
    companion object {
        const val SEND_TOKEN_DATA = "SEND_TOKEN_DATA"
    }

    private val proxyRepository = ProxyRepository()
    private var accountNonceRequest: BackendRequest<AccountNonce>? = null
    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null

    var sendTokenData = SendTokenData()
    val tokens: MutableLiveData<List<Token>> by lazy { MutableLiveData<List<Token>>() }
    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val transactionReady: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val feeReady: MutableLiveData<Long> by lazy { MutableLiveData<Long>() }
    val defaultToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val showAuthentication: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    fun dispose() {
        accountNonceRequest?.dispose()
        globalParamsRequest?.dispose()
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
                    tokensFound.addAll(tokens.map { Token(it.tokenId, "", "", null, false, "") })
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
        waiting.postValue(true)
        proxyRepository.getTransferCost(type = ProxyRepository.SIMPLE_TRANSFER,
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
                handleBackendError(it)
            })
    }

    fun loadCCDDefaultToken(accountAddress: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val accountRepository = AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
            val account = accountRepository.findByAddress(accountAddress)
            val atDisposal = account?.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance) ?: 0
            defaultToken.postValue(Token("", "CCD", "", null, false, "", true, account?.totalBalance ?: 0, atDisposal))
        }
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

    private fun getAccountEncryptedKey(credentialsOutput: StorageAccountData) {
        proxyRepository.getAccountEncryptedKey(
            sendTokenData.receiver,
            {
                sendTokenData.receiverPublicKey = it.accountEncryptionKey
                createTransaction(credentialsOutput.accountKeys, credentialsOutput.encryptionSecretKey)
            },
            {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private fun createTransaction(keys: AccountData, encryptionSecretKey: String) {

        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000

        viewModelScope.launch {
            val serializeTokenTransferParametersInput = SerializeTokenTransferParametersInput(
                tokenId = sendTokenData.token?.token ?: "",
                amount = sendTokenData.amount.toString(),
                from = sendTokenData.account?.address ?: "",
                to = sendTokenData.receiver)
            val serializeTokenTransferParametersOutput = App.appCore.cryptoLibrary.serializeTokenTransferParameters(serializeTokenTransferParametersInput)
/*
            val accountTransactionInput = CreateAccountTransactionInput(
                expiry.toInt(),
                sendTokenData.account?.address ?: "",
                keys,
                sendTokenData.accountNonce?.nonce ?: -1,
                serializeTokenTransferParametersOutput?.parameter ?: "",
                CryptoLibrary.REGULAR_TRANSFER)
*/
            //val createTransferOutput = CreateTransferOutput("", "", "", serializeTokenTransferParametersOutput?.parameter ?: "")

        }


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

        sendTokenData.expiry = expiry
        val transferInput = CreateTransferInput(
            sendTokenData.account?.address ?: "",
            keys,
            toAddress,
            expiry,
            amount.toString(),
            energy,
            nonce.nonce,
            memo,
            sendTokenData.globalParams,
            sendTokenData.receiverPublicKey,
            encryptionSecretKey,
            null, //calculateInputEncryptedAmount(),
            null)

        val transactionType = CryptoLibrary.REGULAR_TRANSFER
        sendTokenData.createTransferInput = transferInput
/*
        val output = App.appCore.cryptoLibrary.createTransfer(transferInput, transactionType)
        if (output == null) {
            waiting.postValue(false)
            errorInt.postValue(R.string.app_error_lib)
        } else {
            sendTokenData.createTransferOutput = output
            submitTransfer(output)
        }*/
    }

    //  //        transactionReady.postValue(true)

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }
}
