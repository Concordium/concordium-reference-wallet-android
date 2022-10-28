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
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toHex
import com.walletconnect.sign.client.Sign
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.Serializable
import javax.crypto.Cipher

data class WalletConnectData(
    var account: Account? = null,
    var wcUri: String? = null,
    var energy: Long? = null,
    var cost: Long? = null
): Serializable

class WalletConnectViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val WALLET_CONNECT_DATA = "WALLET_CONNECT_DATA"
    }

    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val accounts: MutableLiveData<List<AccountWithIdentity>> by lazy { MutableLiveData<List<AccountWithIdentity>>() }
    val chooseAccount: MutableLiveData<AccountWithIdentity> by lazy { MutableLiveData<AccountWithIdentity>() }
    val pair: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val connect: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val decline: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val reject: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val transaction: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val connectStatus: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val serviceName: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val permissions: MutableLiveData<List<String>> by lazy { MutableLiveData<List<String>>() }
    val transactionFee: MutableLiveData<Long> by lazy { MutableLiveData<Long>() }
    val transactionSubmittedOkay: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val showAuthentication: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val errorWalletProxy: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val errorWalletConnect: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val errorWalletConnectApprove: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val errorWalletRejectApprove: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val walletConnectData = WalletConnectData()
    var binder: WalletConnectService.LocalBinder? = null

    private val accountRepository = AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
    private val proxyRepository = ProxyRepository()

    private var accountNonceRequest: BackendRequest<AccountNonce>? = null
    private var accountNonce: AccountNonce? = null

    fun register() {
        EventBus.getDefault().register(this)
    }

    fun unregister() {
        EventBus.getDefault().unregister(this)
    }

    fun pair() {
        binder?.pair(walletConnectData.wcUri ?: "")
    }

    fun ping() {
        binder?.ping()
    }

    fun approveSession() {
        binder?.approveSession(walletConnectData.account!!.address)
    }

    fun rejectSession() {
        binder?.rejectSession()
    }

    fun disconnect() {
        binder?.disconnect()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            accounts.value = accountRepository.getAllDoneWithIdentity()
        }
    }

    suspend fun hasAccounts(): Boolean {
        return accountRepository.getCount() > 0
    }

    fun prepareTransaction() {
        waiting.postValue(true)
        accountNonceRequest?.dispose()
        accountNonceRequest = walletConnectData.account?.let { account ->
            proxyRepository.getAccountNonce(account.address,
                { accountNonce ->
                    this.accountNonce = accountNonce
                    showAuthentication.postValue(true)
                    waiting.postValue(false)
                },
                {
                    waiting.postValue(false)
                    handleBackendError(it)
                }
            )
        }
    }

    fun loadTransactionFee() {
        binder?.getSessionRequestParams()?.parsePayload()?.let { payload ->
            proxyRepository.getTransferCost(type = "update",
                amount = payload.amount.microGtuAmount.toLong(),
                sender = walletConnectData.account!!.address,
                contractIndex = payload.contractAddress.index.toInt(),
                contractSubindex = payload.contractAddress.subindex.toInt(),
                receiveName = payload.receiveName,
                parameter = (binder?.getSessionRequestParamsAsString() ?: "").toByteArray().toHex(),
                success = {
                    walletConnectData.energy = it.energy
                    walletConnectData.cost = it.cost.toLong()
                    transactionFee.postValue(walletConnectData.cost)
                },
                failure = {
                    handleBackendError(it)
                }
            )
        }
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorWalletProxy.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
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
        val jsonSigned = ""
        binder?.approveTransaction(jsonSigned)
    }

    fun sessionName() : String {
        return binder?.getSessionName() ?: ""
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(sessionProposal: Sign.Model.SessionProposal) {
        serviceName.postValue(sessionProposal.name)
        val firstNameSpace = sessionProposal.requiredNamespaces.entries.firstOrNull()
        if (firstNameSpace != null) {
            permissions.postValue(firstNameSpace.value.methods)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(connectionState: ConnectionState) {
        connectStatus.postValue(connectionState.isConnected)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(sessionRequest: Sign.Model.SessionRequest) {
        transaction.postValue(true)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(pairError: PairError) {
        errorWalletConnect.postValue(pairError.message)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(approveError: ApproveError) {
        errorWalletConnectApprove.postValue(approveError.message)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(rejectError: RejectError) {
        errorWalletRejectApprove.postValue(rejectError.message)
    }
}
