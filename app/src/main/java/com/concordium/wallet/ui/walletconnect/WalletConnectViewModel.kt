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

    fun connect() {
        if (walletConnectData.wcUri.isNullOrBlank()) {
            //val uriString = walletConnectData.wcUri.replace("wc:", "wc://")
            //val sessionId = Uri.parse(uriString).userInfo
            //println("LC -> $sessionId")

            wcInit("Concordium Wallet", "Q", walletConnectData.wcUri!!)

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

    fun wcInit(appName: String, appDescription: String, wcUri: String) {
        val appMetaData = Sign.Model.AppMetaData(
            name = appName,
            description = appDescription,
            url = "https://concordium.com",
            icons = listOf(),
            null
        )
        val connectionType = Sign.ConnectionType.AUTOMATIC
        val initParams = Sign.Params.Init(getApplication(), "", appMetaData, null, connectionType)
        SignClient.initialize(initParams) {
            println("LC -> $it")
            wcPair()
        }
    }

    private fun wcPair() {
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

        val pair = Sign.Params.Pair(walletConnectData.wcUri!!)
        SignClient.pair(pair) {
            println("LC -> $it")
        }
    }
}
