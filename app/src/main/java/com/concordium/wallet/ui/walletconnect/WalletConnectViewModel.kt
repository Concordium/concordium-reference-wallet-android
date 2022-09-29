package com.concordium.wallet.ui.walletconnect

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.google.gson.Gson
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import kotlinx.coroutines.launch
import java.io.Serializable
import javax.crypto.Cipher

data class WalletConnectData(
    var account: Account? = null,
    var wcUri: String? = null,
    var sessionProposalJsonString: String? = null,
    var sessionTopic: String? = null,
    var energy: Long? = null,
    var accountNonce: AccountNonce? = null
): Serializable

class WalletConnectViewModel(application: Application) : AndroidViewModel(application), SignClient.WalletDelegate {
    companion object {
        const val WALLET_CONNECT_DATA = "WALLET_CONNECT_DATA"
    }

    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val accounts: MutableLiveData<List<AccountWithIdentity>> by lazy { MutableLiveData<List<AccountWithIdentity>>() }
    val chooseAccount: MutableLiveData<AccountWithIdentity> by lazy { MutableLiveData<AccountWithIdentity>() }
    val connect: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val decline: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val reject: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val transaction: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val connectStatus: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val serviceName: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val permissions: MutableLiveData<List<String>> by lazy { MutableLiveData<List<String>>() }
    val transactionSubmittedOkay: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val showAuthentication: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val errorWalletProxy: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val errorWalletConnect: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val errorWalletConnectApprove: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val walletConnectData = WalletConnectData()

    private val accountRepository = AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
    private val proxyRepository = ProxyRepository()

    private var accountNonceRequest: BackendRequest<AccountNonce>? = null

    fun pair() {
        walletConnectData.wcUri?.let { wc ->
            if (wc.isNotBlank()) {
                pair(wc)
            }
        }
    }

    fun loadAccounts() {
        viewModelScope.launch {
            accounts.value = accountRepository.getAllDoneWithIdentity()
        }
    }

    suspend fun hasAccounts(): Boolean {
        return accountRepository.getCount() > 0
    }

    private fun pair(wc: String) {
        SignClient.setWalletDelegate(this)
        val pairParams = Sign.Params.Pair(wc)
        println("LC -> CALL PAIR")
        SignClient.pair(pairParams) { modelError ->
            println("LC -> PAIR ${modelError.throwable.stackTraceToString()}")
            errorWalletConnect.postValue(modelError.throwable.message)
        }
    }

    fun approve() {
        sessionProposal()?.let { sessionProposal ->
            val firstNameSpace = sessionProposal.requiredNamespaces.entries.firstOrNull()
            if (firstNameSpace != null) {
                val firstNameSpaceKey = firstNameSpace.key
                val firstChain = firstNameSpace.value.chains.firstOrNull()
                val methods = firstNameSpace.value.methods
                val events = firstNameSpace.value.events

                val accounts = listOf( "$firstChain:${walletConnectData.account!!.address}" )
                val namespaceValue = Sign.Model.Namespace.Session(accounts = accounts, methods = methods, events = events, extensions = null)
                val sessionNamespaces = mapOf(Pair(firstNameSpaceKey, namespaceValue))

                val approveParams = Sign.Params.Approve(
                    proposerPublicKey = sessionProposal.proposerPublicKey,
                    namespaces = sessionNamespaces
                )
                println("LC -> CALL APPROVE")
                SignClient.approveSession(approveParams) { modelError ->
                    println("LC -> APPROVE ${modelError.throwable.stackTraceToString()}")
                    errorWalletConnectApprove.postValue(modelError.throwable.message)
                }
            }
        }
    }

    fun prepareTransaction() {
        //if (bakerDelegationData.amount == null && bakerDelegationData.type != ProxyRepository.UPDATE_BAKER_KEYS && bakerDelegationData.type != ProxyRepository.UPDATE_BAKER_POOL) {
        //    _errorLiveData.value = Event(R.string.app_error_general)
        //    return
        //}
        getAccountNonce()
    }

    fun loadTransactionFee() {
        walletConnectData.energy = 1
    }

    private fun getAccountNonce() {
        waiting.postValue(true)
        accountNonceRequest?.dispose()
        accountNonceRequest = walletConnectData.account?.let { account ->
            proxyRepository.getAccountNonce(account.address,
                { accountNonce ->
                    walletConnectData.accountNonce = accountNonce
                    showAuthentication.postValue(true)
                    waiting.postValue(false)
                },
                {
                    waiting.postValue(false)
                    errorWalletProxy.postValue(BackendErrorHandler.getExceptionStringRes(it))
                }
           )
        }
    }

    fun getCipherForBiometrics(): Cipher? {
        return try {
            val cipher =
                App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForDecryption()
            if (cipher == null) {
                errorWalletProxy.postValue(R.string.app_error_keystore_key_invalidated)
            }
            cipher
        } catch (e: KeystoreEncryptionException) {
            errorWalletProxy.postValue(R.string.app_error_keystore)
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
            errorWalletProxy.postValue(R.string.app_error_encryption)
            waiting.postValue(false)
        }
    }

    private suspend fun decryptAndContinue(password: String) {
        walletConnectData.account?.let { account ->
            val storageAccountDataEncrypted = account.encryptedAccountData
            if (TextUtils.isEmpty(storageAccountDataEncrypted)) {
                errorWalletProxy.postValue(R.string.app_error_general)
                waiting.postValue(false)
                return
            }
            val decryptedJson = App.appCore.getCurrentAuthenticationManager().decryptInBackground(password, storageAccountDataEncrypted)
            if (decryptedJson != null) {
                val credentialsOutput = App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
                createTransaction(credentialsOutput.accountKeys, credentialsOutput.encryptionSecretKey)
            } else {
                errorWalletProxy.postValue(R.string.app_error_encryption)
                waiting.postValue(false)
            }
        }
    }

    private suspend fun createTransaction(keys: AccountData, encryptionSecretKey: String?) {

    }

    fun ping() {
        walletConnectData.sessionTopic?.let { topic ->
            val pingParams = Sign.Params.Ping(topic)
            println("LC -> CALL PING")
            SignClient.ping(pingParams, object : Sign.Listeners.SessionPing {
                override fun onSuccess(pingSuccess: Sign.Model.Ping.Success) {
                    println("LC -> PING SUCCESS   ${pingSuccess.topic}")
                    //connectStatus.postValue(true)
                }
                override fun onError(pingError: Sign.Model.Ping.Error) {
                    println("LC -> PING ERROR ${pingError.error.stackTraceToString()}")
                    //connectStatus.postValue(false)
                }
            })
        }
    }

    fun disconnect() {
        walletConnectData.sessionTopic?.let { topic ->
            val disconnectParams = Sign.Params.Disconnect(topic)
            println("LC -> CALL DISCONNECT with $topic")
            SignClient.disconnect(disconnectParams) { modelError ->
                println("LC -> DISCONNECT ${modelError.throwable.stackTraceToString()}")
            }
        }
    }

    fun sessionName() : String {
        sessionProposal()?.let {
            return it.name
        }
        return ""
    }

    private fun sessionProposal(): Sign.Model.SessionProposal? {
        walletConnectData.sessionProposalJsonString?.let {
            return Gson().fromJson(it, Sign.Model.SessionProposal::class.java)
        }
        return null
    }

    override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
        println("LC -> onConnectionStateChange")
        connectStatus.postValue(state.isAvailable)
    }

    override fun onError(error: Sign.Model.Error) {
        println("LC -> onError")
        connectStatus.postValue(false)
    }

    override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
        println("LC -> onSessionDelete")
        connectStatus.postValue(false)
    }

    override fun onSessionProposal(sessionProposal: Sign.Model.SessionProposal) {
        println("LC -> onSessionProposal")
        walletConnectData.sessionProposalJsonString = Gson().toJson(sessionProposal)
        val firstNameSpace = sessionProposal.requiredNamespaces.entries.firstOrNull()
        if (firstNameSpace != null) {
            serviceName.postValue(sessionProposal.name)
            permissions.postValue(firstNameSpace.value.methods)
        }
    }

    override fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest) {
        println("LC -> onSessionRequest")
        transaction.postValue(true)
    }

    override fun onSessionSettleResponse(settleSessionResponse: Sign.Model.SettledSessionResponse) {
        if (settleSessionResponse is Sign.Model.SettledSessionResponse.Result) {
            println("LC -> onSessionSettleResponse SUCCESS -> ${settleSessionResponse.session.topic}")
            walletConnectData.sessionTopic = settleSessionResponse.session.topic
            connectStatus.postValue(true)
        }
        else if (settleSessionResponse is Sign.Model.SettledSessionResponse.Error) {
            println("LC -> onSessionSettleResponse ERROR -> ${settleSessionResponse.errorMessage}")
        }
    }

    override fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse) {
        println("LC -> onSessionUpdateResponse")
    }
}
