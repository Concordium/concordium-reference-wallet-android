package com.concordium.wallet.ui.walletconnect

import android.app.Application
import android.net.Uri
import android.util.Log
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
import java.net.URI
import java.net.URISyntaxException

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

    fun connect() {

        // Example: "wc:bdebc8b0ff3e0b78310e3dc382af729ab3f1d984497c572b4a800ec1e54737d8@2?relay-protocol=waku&symKey=ea043e7e58271714b8a029aacc87679cbee0170f7616229d0f70cb066d9aee32"

        walletConnectData.wcUri?.let { wc ->
            if (wc.isNotBlank()) {
                wcInit(wc)
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

    private fun wcInit(wc: String) {

        val projectId = "76324905a70fe5c388bab46d3e0564dc"
        val relayServerUrl = "wss://relay.walletconnect.com?projectId=$projectId"

        val initString = Sign.Params.Init(
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

        SignClient.initialize(initString) { signModelError ->
            println("LC -> INIT ${signModelError.throwable.stackTraceToString()}")
        }

        wcPair(wc)
    }

    private fun wcPair(wc: String) {
        val walletDelegate = object : SignClient.WalletDelegate {
            override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
                println("LC -> onConnectionStateChange")
                // har set denne, når jeg går tilbage til MainActivity
            }

            override fun onError(error: Sign.Model.Error) {
                println("LC -> onError")
            }

            override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
                println("LC -> onSessionDelete")
            }

            override fun onSessionProposal(sessionProposal: Sign.Model.SessionProposal) {
                println("LC -> onSessionProposal")
            }

            override fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest) {
                println("LC -> onSessionRequest")
            }

            override fun onSessionSettleResponse(settleSessionResponse: Sign.Model.SettledSessionResponse) {
                println("LC -> onSessionSettleResponse")
            }

            override fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse) {
                println("LC -> onSessionUpdateResponse")
            }
        }

        SignClient.setWalletDelegate(walletDelegate)

        val pair = Sign.Params.Pair(wc)
        SignClient.pair(pair) { signModelError ->
            println("LC -> PAIR ${signModelError.throwable.stackTraceToString()}")

            // Første gang får jeg NoRelayConnectionException
            // Bagefter får jeg PairWithExistingPairingIsNotAllowed - noget er gemt i database og filer - så sker dette!

            //val disconnect = Sign.Params.Disconnect(sessionTopic = "")
            //SignClient.disconnect(disconnect) { error ->
            //    println("LC -> DISCONNECT ${error.throwable.stackTraceToString()}")
            //}
        }

        //SignClient.disconnect()
    }
}
