package com.concordium.wallet.ui.walletconnect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.WalletDatabase
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import kotlinx.coroutines.launch
import java.io.Serializable

data class WalletConnectData(
    var account: Account? = null,
    var wcUri: String? = null,
    var sessionProposal: Sign.Model.SessionProposal? = null
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
    val connectStatus: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val serviceName: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val permissions: MutableLiveData<List<String>> by lazy { MutableLiveData<List<String>>() }
    val transactionSubmittedOkay: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val walletConnectData = WalletConnectData()
    private val accountRepository = AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
    private var settleSessionResponseResult: Sign.Model.SettledSessionResponse.Result? = null
    private var settleSessionResponseError: Sign.Model.SettledSessionResponse.Error? = null
    private var sessionTopic: String? = null

    fun pairWalletConnect() {
        walletConnectData.wcUri?.let { wc ->
            if (wc.isNotBlank()) {
                pairWalletConnect(wc)
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

    private fun pairWalletConnect(wc: String) {
        SignClient.setWalletDelegate(this)
        val pairParams = Sign.Params.Pair(wc)
        println("LC -> CALL PAIR")
        SignClient.pair(pairParams) { modelError ->
            println("LC -> PAIR ${modelError.throwable.stackTraceToString()}")
        }
    }

    fun approve() {
        walletConnectData.sessionProposal?.let { sessionProposal ->
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
                }
            }
        }
    }

    fun submit() {

    }

    private fun ping() {
        settleSessionResponseResult?.let { settleSessionResponse ->
            val pingParams = Sign.Params.Ping(settleSessionResponse.session.topic)
            println("LC -> CALL PING")
            SignClient.ping(pingParams, object : Sign.Listeners.SessionPing {
                override fun onSuccess(pingSuccess: Sign.Model.Ping.Success) {
                    println("LC -> PING SUCCESS   ${pingSuccess.topic}")
                    sessionTopic = pingSuccess.topic
                    connectStatus.postValue(true)
                }
                override fun onError(pingError: Sign.Model.Ping.Error) {
                    println("LC -> PING ERROR ${pingError.error.stackTraceToString()}")
                    connectStatus.postValue(false)
                }
            })
        }
    }

    fun disconnectWalletConnect() {
        sessionTopic?.let { topic ->
            val disconnectParams = Sign.Params.Disconnect(topic)
            println("LC -> CALL DISCONNECT with $topic")
            SignClient.disconnect(disconnectParams) { modelError ->
                println("LC -> DISCONNECT ${modelError.throwable.stackTraceToString()}")
            }
            ping()
        }
    }

    fun sessionName() : String {
        return walletConnectData.sessionProposal?.name ?: ""
    }

    override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
        println("LC -> onConnectionStateChange")
    }

    override fun onError(error: Sign.Model.Error) {
        println("LC -> onError")
    }

    override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
        println("LC -> onSessionDelete")
    }

    override fun onSessionProposal(sessionProposal: Sign.Model.SessionProposal) {
        println("LC -> onSessionProposal")
        walletConnectData.sessionProposal = sessionProposal
        val firstNameSpace = sessionProposal.requiredNamespaces.entries.firstOrNull()
        if (firstNameSpace != null) {
            serviceName.postValue(sessionProposal.name)
            permissions.postValue(firstNameSpace.value.methods)
        }
    }

    override fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest) {
        println("LC -> onSessionRequest")
    }

    override fun onSessionSettleResponse(settleSessionResponse: Sign.Model.SettledSessionResponse) {
        println("LC -> onSessionSettleResponse")
        if (settleSessionResponse is Sign.Model.SettledSessionResponse.Result) {
            this.settleSessionResponseResult = settleSessionResponse
            ping()
        }
        else if (settleSessionResponse is Sign.Model.SettledSessionResponse.Error) {
            this.settleSessionResponseError = settleSessionResponse
        }
    }

    override fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse) {
        println("LC -> onSessionUpdateResponse")
    }
}
