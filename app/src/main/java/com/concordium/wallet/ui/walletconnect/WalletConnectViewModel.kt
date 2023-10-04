package com.concordium.wallet.ui.walletconnect

import android.app.Application
import android.os.CountDownTimer
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
import com.concordium.wallet.data.cryptolib.CreateAccountTransactionInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.ParameterToJsonInput
import com.concordium.wallet.data.cryptolib.SignMessageInput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.SubmissionData
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.data.walletconnect.Payload
import com.concordium.wallet.data.walletconnect.TransactionError
import com.concordium.wallet.data.walletconnect.TransactionSuccess
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.PrettyPrint.prettyPrint
import com.concordium.wallet.util.toBigInteger
import com.google.gson.Gson
import com.walletconnect.sign.client.Sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.Serializable
import java.math.BigInteger
import java.util.Locale
import javax.crypto.Cipher

data class WalletConnectData(
    var account: Account? = null,
    var wcUri: String? = null,
    var energy: Long? = null,
    var maxEnergy: Long? = null,
    var cost: BigInteger? = null,
    var isTransaction: Boolean = true
) : Serializable

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
    val transactionSubmittedSuccess: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val transactionSubmittedError: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val transactionSubmittedOkay: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val message: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val connectStatus: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val serviceName: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val permissions: MutableLiveData<List<String>> by lazy { MutableLiveData<List<String>>() }
    val transactionFee: MutableLiveData<BigInteger> by lazy { MutableLiveData<BigInteger>() }
    val messagedSignedOkay: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val showAuthentication: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val messageSignedSuccess: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val messageSignedError: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val errorString: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val jsonPretty: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val errorWalletConnectApprove: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val walletConnectData = WalletConnectData()
    var binder: WalletConnectService.LocalBinder? = null

    private val accountUpdater = AccountUpdater(application, viewModelScope)
    private val accountRepository =
        AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
    private val proxyRepository = ProxyRepository()
    private var submitTransaction: BackendRequest<SubmissionData>? = null
    private var accountNonceRequest: BackendRequest<AccountNonce>? = null
    private var accountNonce: AccountNonce? = null
    private var accountUpdaterTimer: CountDownTimer? = null

    fun register() {
        EventBus.getDefault().register(this)
    }

    fun unregister() {
        EventBus.getDefault().unregister(this)
    }

    fun pair() {
        binder?.pair(walletConnectData.wcUri ?: "")
    }

    fun approveSession() {
        binder?.approveSession(walletConnectData.account!!.address)
    }

    fun rejectSession() {
        binder?.rejectSession()
    }

    fun respondSuccess(message: String) {
        binder?.respondSuccess(message)
    }

    fun respondError(message: String) {
        binder?.respondError(message)
    }

    fun disconnect() {
        binder?.disconnect()
        accountUpdaterTimer?.cancel()
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
            when (payload) {
                is Payload.ContractUpdateTransaction -> {
                    proxyRepository.getTransferCost(
                        type = "update",
                        amount = payload.amount.toBigInteger(),
                        sender = walletConnectData.account!!.address,
                        contractIndex = payload.address.index,
                        contractSubindex = payload.address.subIndex,
                        receiveName = payload.receiveName,
                        parameter = payload.message,
                        success = {
                            walletConnectData.maxEnergy =
                                if (payload.maxEnergy != 0L) payload.maxEnergy else payload.maxContractExecutionEnergy
                            walletConnectData.cost = it.cost.toBigInteger()
                            walletConnectData.energy = it.energy
                            transactionFee.postValue(walletConnectData.cost)
                        },
                        failure = {
                            handleBackendError(it)
                        }
                    )
                }

                is Payload.AccountTransaction -> {
                    proxyRepository.getTransferCost(
                        type = ProxyRepository.SIMPLE_TRANSFER,
                        memoSize = null,
                        success = {
                            walletConnectData.energy = it.energy
                            walletConnectData.cost = it.cost.toBigInteger()
                            transactionFee.postValue(walletConnectData.cost)
                        },
                        failure = {
                            handleBackendError(it)
                        }
                    )
                }
            }
        }
    }

    fun hasEnoughFunds(): Boolean {
        val payload = binder?.getSessionRequestParams()?.parsePayload()
        val amount =
            when (payload) {
                is Payload.ContractUpdateTransaction -> payload.amount.toBigInteger()

                is Payload.AccountTransaction -> payload.amount.toBigInteger()

                else -> BigInteger.ZERO
            }
        val fee = walletConnectData.cost
        if (amount != null && fee != null) {
            walletConnectData.account?.totalUnshieldedBalance?.let { totalUnshieldedBalance ->
                walletConnectData.account?.getAtDisposalWithoutStakedOrScheduled(
                    totalUnshieldedBalance
                )?.let { atDisposal ->
                    if (atDisposal >= amount + fee)
                        return true
                }
            }
        }
        return false
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
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
        walletConnectData.account?.let { account ->
            val storageAccountDataEncrypted = account.encryptedAccountData
            if (TextUtils.isEmpty(storageAccountDataEncrypted)) {
                errorInt.postValue(R.string.app_error_general)
                waiting.postValue(false)
                return
            }
            val decryptedJson = App.appCore.getCurrentAuthenticationManager()
                .decryptInBackground(password, storageAccountDataEncrypted)
            if (decryptedJson != null) {
                val credentialsOutput =
                    App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
                if (walletConnectData.isTransaction)
                    createTransaction(credentialsOutput.accountKeys)
                else
                    signMessage(credentialsOutput.accountKeys)
            } else {
                errorInt.postValue(R.string.app_error_encryption)
                waiting.postValue(false)
            }
        }
    }

    private suspend fun createTransaction(keys: AccountData) {
        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000
        val from = walletConnectData.account?.address ?: ""
        val nonce = this.accountNonce?.nonce
        val payload = binder?.getSessionRequestParams()?.parsePayload()
        val type = binder?.getSessionRequestParams()?.type

        if (from.isBlank() || nonce == null || payload == null || type == null) {
            errorInt.postValue(R.string.app_error_lib)
            return
        }
        if (payload is Payload.ContractUpdateTransaction) {
            payload.maxEnergy = walletConnectData.energy ?: 0
            payload.maxContractExecutionEnergy = walletConnectData.maxEnergy ?: 0
        }
        val accountTransactionInput = CreateAccountTransactionInput(
            expiry.toInt(),
            from,
            keys,
            this.accountNonce?.nonce ?: -1,
            payload,
            type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
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

    fun sessionName(): String {
        return binder?.getSessionName() ?: ""
    }

    fun prepareMessage() {
        showAuthentication.postValue(true)
    }

    fun prettyPrintJson() {

        binder?.getSessionRequestParams()?.let { params ->
            val payloadObj = params.parsePayload()
            val schema = params.schema
            if (payloadObj != null && schema != null && payloadObj is Payload.ContractUpdateTransaction) {
                CoroutineScope(Dispatchers.IO).launch {
                    val jsonMessage = App.appCore.cryptoLibrary.parameterToJson(
                        ParameterToJsonInput(
                            payloadObj.message,
                            payloadObj.receiveName,
                            schema,
                            null
                        )
                    )
                    if (jsonMessage != null) {
                        jsonPretty.postValue(jsonMessage.prettyPrint())
                    } else {
                        jsonPretty.postValue("")
                    }
                }
            } else {
                jsonPretty.postValue("")
            }
        }
    }

    private fun submitTransaction(createTransferOutput: CreateTransferOutput) {
        waiting.postValue(true)
        submitTransaction = proxyRepository.submitTransfer(createTransferOutput,
            {
                println("LC -> submitTransaction SUCCESS = ${it.submissionId}")
                transactionSubmittedSuccess.postValue(Gson().toJson(TransactionSuccess(it.submissionId)))
                waiting.postValue(false)
                initializeAccountUpdater()
            },
            {
                println("LC -> submitTransaction ERROR ${it.stackTraceToString()}")
                transactionSubmittedError.postValue(
                    Gson().toJson(
                        TransactionError(
                            500,
                            it.message ?: ""
                        )
                    )
                )
                handleBackendError(it)
                waiting.postValue(false)
            }
        )
    }

    private fun signMessage(keys: AccountData) {
        viewModelScope.launch {
            val signMessageInput = SignMessageInput(
                walletConnectData.account?.address ?: "",
                binder?.getSessionRequestParams()?.message ?: "",
                keys
            )
            val signMessageOutput = App.appCore.cryptoLibrary.signMessage(signMessageInput)
            if (signMessageOutput == null) {
                errorInt.postValue(R.string.app_error_lib)
                messageSignedError.postValue(Gson().toJson(TransactionError(500, "")))
            } else {
                messageSignedSuccess.postValue(Gson().toJson(signMessageOutput))
            }
        }
    }

    private fun initializeAccountUpdater() {
        accountUpdater.setUpdateListener(object : AccountUpdater.UpdateListener {
            override fun onDone(totalBalances: TotalBalancesData) {
                walletConnectData.account?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        walletConnectData.account = accountRepository.findById(it.id)
                    }
                }
            }

            override fun onError(stringRes: Int) {}
            override fun onNewAccountFinalized(accountName: String) {}
        })
        accountUpdaterTimer = object : CountDownTimer(Long.MAX_VALUE, 5000) {
            override fun onTick(millisUntilFinished: Long) {
                walletConnectData.account?.let { accountUpdater.updateForAccount(it) }
            }

            override fun onFinish() {}
        }
        accountUpdaterTimer?.start()
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
        if (sessionRequest.request.method == "sign_and_send_transaction")
            transaction.postValue(true)
        else if (sessionRequest.request.method == "sign_message")
            message.postValue(true)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(pairError: PairError) {
        errorString.postValue(pairError.message)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(approveError: ApproveError) {
        errorWalletConnectApprove.postValue(approveError.message)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(rejectError: RejectError) {
        errorString.postValue(rejectError.message)
    }
}
