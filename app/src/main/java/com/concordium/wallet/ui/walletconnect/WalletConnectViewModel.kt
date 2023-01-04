package com.concordium.wallet.ui.walletconnect

import android.app.Application
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.AppConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.*
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.data.walletconnect.TransactionError
import com.concordium.wallet.data.walletconnect.TransactionSuccess
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.ui.walletconnect.proof.of.identity.ProofOfIdentityHelper
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toHex
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.walletconnect.sign.client.Sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.Serializable
import java.util.*
import javax.crypto.Cipher
import kotlin.collections.ArrayList

data class WalletConnectData(
    var account: Account? = null,
    var wcUri: String? = null,
    var energy: Long? = null,
    var cost: Long? = null,
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
    val transactionSubmittedSuccess: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val transactionSubmittedError: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val transactionSubmittedOkay: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val transaction: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val message: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val proofOfIdentity: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val proofOfIdentityRequest: MutableLiveData<ProofOfIdentity> by lazy { MutableLiveData<ProofOfIdentity>() }
    val proofOfIdentityCheck: MutableLiveData<Proofs> by lazy { MutableLiveData<Proofs>() }
    val transactionAction: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val messageAction: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val proofOfIdentityAction: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val connectStatus: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val serviceName: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val permissions: MutableLiveData<List<String>> by lazy { MutableLiveData<List<String>>() }
    val transactionFee: MutableLiveData<Long> by lazy { MutableLiveData<Long>() }
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

    val stepPageBy: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    private val accountUpdater = AccountUpdater(application, viewModelScope)
    private val accountRepository =
        AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
    private val transferRepository =
        TransferRepository(WalletDatabase.getDatabase(application).transferDao())
    private val proxyRepository = ProxyRepository()
    private var submitTransaction: BackendRequest<SubmissionData>? = null
    private var accountNonceRequest: BackendRequest<AccountNonce>? = null
    private var transferSubmissionStatusRequest: BackendRequest<TransferSubmissionStatus>? = null
    private var accountNonce: AccountNonce? = null
    private var accountUpdaterTimer: CountDownTimer? = null
    private var transferSubmissionStatus: TransferSubmissionStatus? = null
    private var submissionId: String? = null

    private val identityProviderRepository: IdentityProviderRepository =
        IdentityProviderRepository()

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
        submitTransaction?.dispose()
        accountNonceRequest?.dispose()
        transferSubmissionStatusRequest?.dispose()
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
            proxyRepository.getTransferCost(type = "update",
                amount = payload.amount.toLong(),
                sender = walletConnectData.account!!.address,
                contractIndex = payload.address.index,
                contractSubindex = payload.address.subIndex,
                receiveName = payload.receiveName,
                parameter = (binder?.getSessionRequestParamsAsString() ?: "").toHex(),
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

    fun hasEnoughFunds(): Boolean {
        val amount = binder?.getSessionRequestParams()?.parsePayload()?.amount
        val fee = walletConnectData.cost
        if (amount != null && fee != null) {
            walletConnectData.account?.totalUnshieldedBalance?.let { totalUnshieldedBalance ->
                walletConnectData.account?.getAtDisposalWithoutStakedOrScheduled(
                    totalUnshieldedBalance
                )?.let { atDisposal ->
                    if (atDisposal >= amount.toLong() + fee.toLong())
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

        payload.maxEnergy = walletConnectData.energy?.toInt() ?: 0
        val accountTransactionInput = CreateAccountTransactionInput(
            expiry.toInt(),
            from,
            keys,
            this.accountNonce?.nonce ?: -1,
            payload,
            type
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
            val strategy: ExclusionStrategy = object : ExclusionStrategy {
                override fun shouldSkipField(f: FieldAttributes): Boolean {
                    return f.name == "payload" || f.name == "schema"
                }

                override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                    return false
                }
            }
            val gson = GsonBuilder().setPrettyPrinting().addSerializationExclusionStrategy(strategy)
                .create()
            params.payloadObj = params.parsePayload()
            params.payload = ""
            if (params.payloadObj != null && params.payloadObj?.message != null && params.schema != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val jsonMessage = App.appCore.cryptoLibrary.parameterToJson(
                        ParameterToJsonInput(
                            params.payloadObj!!.message,
                            params.payloadObj!!.receiveName,
                            params.schema!!,
                            null
                        )
                    )
                    if (jsonMessage != null) {
                        jsonPretty.postValue(gson.toJson(JsonParser.parseString(jsonMessage)))
                    } else {
                        jsonPretty.postValue(gson.toJson(params).replace("payloadObj", "payload"))
                    }
                }
            } else {
                jsonPretty.postValue(gson.toJson(params).replace("payloadObj", "payload"))
            }
        }
    }

    private fun submitTransaction(createTransferOutput: CreateTransferOutput) {
        waiting.postValue(true)
        submitTransaction?.dispose()
        submitTransaction = proxyRepository.submitTransfer(createTransferOutput,
            {
                println("LC -> submitTransaction SUCCESS = ${it.submissionId}")
                this.submissionId = it.submissionId
                submissionStatus()
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

    private fun submissionStatus() {
        waiting.postValue(true)
        transferSubmissionStatusRequest?.dispose()
        transferSubmissionStatusRequest = submissionId?.let { submissionId ->
            proxyRepository.getTransferSubmissionStatus(submissionId,
                { transferSubmissionStatus ->
                    this.transferSubmissionStatus = transferSubmissionStatus
                    finishTransferCreation()
                },
                {
                    waiting.postValue(false)
                    handleBackendError(it)
                }
            )
        }
    }

    private fun finishTransferCreation() {
        val createdAt = Date().time
        val accountId = walletConnectData.account?.id
        val fromAddress = walletConnectData.account?.address
        val submissionId = this.submissionId
        val transferSubmissionStatus = this.transferSubmissionStatus
        val cost = walletConnectData.cost
        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000

        if (transferSubmissionStatus == null || cost == null || accountId == null || fromAddress == null || submissionId == null) {
            errorInt.postValue(R.string.app_error_general)
            waiting.postValue(false)
            return
        }

        val transfer = Transfer(
            0,
            accountId,
            cost,
            0,
            fromAddress,
            fromAddress,
            expiry,
            "",
            createdAt,
            submissionId,
            TransactionStatus.UNKNOWN,
            TransactionOutcome.UNKNOWN,
            TransactionType.TRANSFER,
            null,
            0,
            null
        )
        saveNewTransfer(transfer)
    }

    private fun saveNewTransfer(transfer: Transfer) = viewModelScope.launch {
        transferRepository.insert(transfer)
        waiting.postValue(false)
        transactionSubmittedSuccess.postValue(Gson().toJson(TransactionSuccess(submissionId ?: "")))
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
        when (sessionRequest.request.method) {
            "sign_and_send_transaction" -> transaction.postValue(sessionRequest.request.method)
            "sign_message" -> message.postValue(sessionRequest.request.method)
            "proof_of_identity" -> proofOfIdentity.postValue(sessionRequest.request.method)
        }
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

    fun validateProofOfIdentity(proofOfIdentity: ProofOfIdentity) {

        if (walletConnectData.account != null) {

            viewModelScope.launch {
                try {
                    val localAccount = accountRepository.getAllDoneWithIdentity()
                        .first { it.identity.id == walletConnectData.account!!.identityId }

                    localAccount.identity.identityProvider.ipInfo

                    localAccount.identity.identityObject?.let {
                        val identityAttributes = it.attributeList.chosenAttributes
                        val proofOfIdentityHelper =
                            ProofOfIdentityHelper(
                                proofOfIdentity.challenge!!,
                                proofOfIdentity,
                                identityAttributes
                            )
                        proofOfIdentityCheck.postValue(proofOfIdentityHelper.getProofs())
                    }
                } catch (_: NoSuchElementException) {
                    Log.e("No Such Identity!")
                }
            }
        }
    }

    fun sendIdentityProof() {
        viewModelScope.launch {
            val proof = proofOfIdentityCheck.value!!
            val challenge = proof.challenge!!.toByteArray()

            val localAccount = accountRepository.getAllDoneWithIdentity()
                .first { it.identity.id == walletConnectData.account!!.identityId }

            val ipInfo = localAccount.identity.identityProvider.ipInfo
            val identityObject = localAccount.identity.identityObject
            val identityIndex = localAccount.identity.identityIndex
            val accountNumber = localAccount.account.credNumber
            val net = AppConfig.net
            val seed = AuthPreferences(getApplication()).getSeedPhrase()
            val statements = proofOfIdentityRequest.value!!.statement

            identityProviderRepository.getIGlobalInfo(
                {
                    val global = it.value
                    val proofInput = ProofsInput(
                        ipInfo,
                        global,
                        identityObject,
                        statements,
                        challenge,
                        identityIndex,
                        accountNumber,
                        seed,
                        net
                    )
                    viewModelScope.launch {
                        App.appCore.cryptoLibrary.proveIdStatement(proofInput)?.let {
                            Log.e("PROOF RESPONSE: $it")

                        }
                    }


                },
                {

                })


/*
            val proofInput = ProofsInput(
                method = "proof_of_identity",
                params = identityParams
            )

            App.appCore.cryptoLibrary.proveIdStatement(proofInput)?.let {
                proofs.add(
                    ProofLocal(
                        proof = it,
                        type = revealProof.type!!.type,
                        attribute = revealProof.rawValue
                    )
                )
            }*/
        }
    }
}
