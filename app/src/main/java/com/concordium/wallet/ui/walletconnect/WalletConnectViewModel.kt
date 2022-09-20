package com.concordium.wallet.ui.walletconnect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.WalletDatabase
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import kotlinx.coroutines.launch
import java.io.Serializable

data class WalletConnectData(
    var account: Account? = null,
    var wcUri: String? = null
): Serializable

class WalletConnectViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val WALLET_CONNECT_DATA = "WALLET_CONNECT_DATA"
    }

    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val accounts: MutableLiveData<List<AccountWithIdentity>> by lazy { MutableLiveData<List<AccountWithIdentity>>() }
    val chooseAccount: MutableLiveData<AccountWithIdentity> by lazy { MutableLiveData<AccountWithIdentity>() }
    val walletConnectData = WalletConnectData()
    private val accountRepository = AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())

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

    /*
    fun initWalletConnect() {
        val projectId = "76324905a70fe5c388bab46d3e0564dc"
        val relayServerUrl = "wss://relay.walletconnect.com?projectId=$projectId"

        val initParams = Sign.Params.Init(
            application = getApplication(),
            relayServerUrl = relayServerUrl,
            metadata = Sign.Model.AppMetaData(
                name = "Concordium Wallet",
                description = "Concordium Wallet description",
                url = "https://concordium.com",
                icons = listOf("https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media"),
                redirect = "kotlin-wallet-wc:/request"
            )
        )

        SignClient.initialize(initParams) { signModelError ->
            println("LC -> INIT ${signModelError.throwable.stackTraceToString()}")
        }
    }
    */

    private fun pairWalletConnect(wc: String) {
        val walletDelegate = object : SignClient.WalletDelegate {
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

                val accounts = listOf( "eip155:1:${walletConnectData.account!!.address}" )
                val methods = sessionProposal.requiredNamespaces.values.flatMap { it.methods }
                val events = sessionProposal.requiredNamespaces.values.flatMap { it.events }
                val namespaceValue = Sign.Model.Namespace.Session(accounts = accounts, methods = methods, events = events, extensions = null)

                val sessionNamespaces = mapOf(Pair("eip155", namespaceValue))

                val approveParams = Sign.Params.Approve(
                    proposerPublicKey = sessionProposal.proposerPublicKey,
                    namespaces = sessionNamespaces
                )
                println("LC -> CALL APPROVE")
                SignClient.approveSession(approveParams) { modelError ->
                    println("LC -> APPROVE ${modelError.throwable.stackTraceToString()}")
                }
            }

            override fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest) {
                println("LC -> onSessionRequest")
            }

            override fun onSessionSettleResponse(settleSessionResponse: Sign.Model.SettledSessionResponse) {
                println("LC -> onSessionSettleResponse") // er nu kommet her til
            }

            override fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse) {
                println("LC -> onSessionUpdateResponse")
            }
        }

        SignClient.setWalletDelegate(walletDelegate)

        val pairParams = Sign.Params.Pair(wc)
        println("LC -> CALL PAIR")
        SignClient.pair(pairParams) { modelError ->
            println("LC -> PAIR ${modelError.throwable.stackTraceToString()}")
        }
    }

    fun disconnectWalletConnect() {
        val sessionTopic = ""
        val disconnectParams = Sign.Params.Disconnect(sessionTopic)
        println("LC -> CALL DISCONNECT")
        SignClient.disconnect(disconnectParams) { modelError ->
            println("LC -> DISCONNECT ${modelError.throwable.stackTraceToString()}")
        }
    }
}
