package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateTransferInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.*
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class DelegationViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var delegationData: DelegationData
    private val proxyRepository = ProxyRepository()
    private var bakerPoolRequest: BackendRequest<BakerPoolStatus>? = null
    private var accountNonceRequest: BackendRequest<AccountNonce>? = null
    private var submitCredentialRequest: BackendRequest<SubmissionData>? = null

    private val gson = App.appCore.gson

    private val _transactionSuccessLiveData = MutableLiveData<Boolean>()
    val transactionSuccessLiveData: LiveData<Boolean>
        get() = _transactionSuccessLiveData

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _showDetailedLiveData = MutableLiveData<Event<Boolean>>()
    val showDetailedLiveData: LiveData<Event<Boolean>>
        get() = _showDetailedLiveData

    private val _transactionFeeLiveData = MutableLiveData<Long>()
    val transactionFeeLiveData: LiveData<Long>
        get() = _transactionFeeLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    fun initialize(delegationData: DelegationData) {
        this.delegationData = delegationData
    }

    fun isBakerPool(): Boolean {
        return this.delegationData.isBakerPool
    }

    fun isLPool(): Boolean {
        return this.delegationData.isLPool
    }

    fun isUpdating(): Boolean {
        delegationData.account?.accountDelegation?.let { return it.stakedAmount.isNotBlank() }
        return false
    }

    fun atDisposal(): Long {
        return (delegationData.account?.finalizedBalance ?: 0) - (delegationData.account?.accountDelegation?.stakedAmount?.toLong() ?: 0)
    }

    fun selectBakerPool() {
        this.delegationData.isLPool = false
        this.delegationData.isBakerPool = true
    }

    fun selectLPool() {
        this.delegationData.isLPool = true
        this.delegationData.isBakerPool = false
    }

    fun markRestake(restake: Boolean) {
        this.delegationData.restake = restake
        loadTransactionFee()
    }

    fun setOldPoolID(id: String) {
        delegationData.oldPoolId = id
    }

    fun getOldPoolId(): String {
        return delegationData.oldPoolId
    }

    fun setPoolID(id: String) {
        delegationData.poolId = id
    }

    fun getPoolId(): String {
        return delegationData.poolId
    }

    fun validatePoolId() {
        if(isLPool()){
            _showDetailedLiveData.value = Event(true)
        }
        else{
            _waitingLiveData.value = true
            bakerPoolRequest?.dispose()
            bakerPoolRequest = proxyRepository.getBakerPool(getPoolId(),
                {
                    delegationData.bakerPoolStatus = it
                    _waitingLiveData.value = false
                    _showDetailedLiveData.value = Event(true)
                },
                {
                    _waitingLiveData.value = false
                    _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
                }
            )
        }
    }

    fun loadTransactionFee() {

        var type = when(delegationData.type) {
            DelegationData.TYPE_REGISTER_DELEGATION -> ProxyRepository.REGISTER_DELEGATION
            DelegationData.TYPE_UPDATE_DELEGATION -> ProxyRepository.UPDATE_DELEGATION
            DelegationData.TYPE_REMOVE_DELEGATION -> ProxyRepository.REMOVE_DELEGATION
            else -> ProxyRepository.REGISTER_DELEGATION
        }

        var amount = when(delegationData.type) {
            DelegationData.TYPE_UPDATE_DELEGATION -> delegationData.amount
            else -> null
        }

        var restake = when(delegationData.type) {
            DelegationData.TYPE_UPDATE_DELEGATION -> delegationData.restake
            else -> null
        }

        proxyRepository.getTransferCost(type,
            null,
            amount,
            restake,
            delegationData.isLPool,
            {
                delegationData.energy = it.energy
                _transactionFeeLiveData.value = it.cost.toLong()
            },
            {
                handleBackendError(it)
            }
        )
    }

    fun loadChainParameters() {
        _waitingLiveData.value = true
        proxyRepository.getChainParameters(
            {
                delegationData.chainParameters = it
                _waitingLiveData.value = false
            },
            {
                _waitingLiveData.value = false
                handleBackendError(it)
            }
        )
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(throwable))
    }

    fun usePasscode(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().usePasscode()
    }

    fun delegateAmount() {
        if (delegationData.amount == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            return
        }
        getAccountNonce()
    }

    private fun getAccountNonce() {
        _waitingLiveData.value = true
        accountNonceRequest?.dispose()
        accountNonceRequest = delegationData.account?.let { account ->
            proxyRepository.getAccountNonce(account.address,
                { accountNonce ->
                    delegationData.accountNonce = accountNonce
                    _showAuthenticationLiveData.value = Event(true)
                    _waitingLiveData.value = false
                },
                {
                    _waitingLiveData.value = false
                    handleBackendError(it)
                }
            )
        }
    }

    fun shouldUseBiometrics(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().useBiometrics()
    }

    fun getCipherForBiometrics(): Cipher? {
        try {
            val cipher =
                App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForDecryption()
            if (cipher == null) {
                _errorLiveData.value = Event(R.string.app_error_keystore_key_invalidated)
            }
            return cipher
        } catch (e: KeystoreEncryptionException) {
            _errorLiveData.value = Event(R.string.app_error_keystore)
            return null
        }
    }

    fun continueWithPassword(password: String) = viewModelScope.launch {
        _waitingLiveData.value = true
        decryptAndContinue(password)
    }

    fun checkLogin(cipher: Cipher) = viewModelScope.launch {
        _waitingLiveData.value = true
        val password =
            App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(cipher)
        if (password != null) {
            decryptAndContinue(password)
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
        }
    }

    private suspend fun decryptAndContinue(password: String) {
        // Decrypt the private data
        Log.d("decryptAndContinue")
        delegationData.account?.let { account ->
            val storageAccountDataEncrypted = account.encryptedAccountData
            if (TextUtils.isEmpty(storageAccountDataEncrypted)) {
                _errorLiveData.value = Event(R.string.app_error_general)
                _waitingLiveData.value = false
                return
            }
            val decryptedJson = App.appCore.getCurrentAuthenticationManager().decryptInBackground(password, storageAccountDataEncrypted)

            if (decryptedJson != null) {
                val credentialsOutput = gson.fromJson(decryptedJson, StorageAccountData::class.java)
                createDelegation(credentialsOutput.accountKeys, credentialsOutput.encryptionSecretKey)
            } else {
                _errorLiveData.value = Event(R.string.app_error_encryption)
                _waitingLiveData.value = false
            }
        }
    }

    private suspend fun createDelegation(keys: AccountData, encryptionSecretKey: String) {
/*

        Mandatory:
        "from" ... address of the sender account.
        "expiry" ... unix timestamp of the expiry date of the transaction.
        "nonce" ... nonce of the sender account.
        "keys" ... mapping with the keys of the sender account.
        "energy" ... max energy wanted for the transfer.

        Optional:
        "capital" ... string containing the amount to be staked.
        "restakeEarnings" ... bool indicating whether earnings should be restaked.
        "delegationTarget" ... JSON indicating either delegation to the L-pool or to a baker pool.

        The delegation target should either be of the form
        {
            "type": "delegateToLPool"
        }
        or

        {
            "type": "delegateToBaker",
            "targetBaker": 100
        }

 */
        val from = delegationData.account?.address
        val nonce = delegationData.accountNonce
        val energy = delegationData.energy

        val capital = delegationData.amount.toString()
        val restakeEarnings = delegationData.restake
        val delegationTarget =
            if(delegationData.isBakerPool){
                DelegationTarget(DelegationTarget.TYPE_DELEGATE_TO_BAKER, delegationData.poolId.toLong())
            }
            else{
                DelegationTarget(DelegationTarget.TYPE_DELEGATE_TO_L_POOL, null)
            }

        if (from == null || nonce == null || keys == null || energy == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return
        }

        //Expiry should me now + 10 minutes (in seconds)
        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000
        val transferInput = CreateTransferInput(
            from,
            keys,
            null,
            expiry,
            null,
            energy,
            nonce.nonce,
            null,
            null,
            null,
            encryptionSecretKey,
            null,
            capital,
            restakeEarnings,
            delegationTarget
        )

        val output = App.appCore.cryptoLibrary.createTransfer(transferInput, CryptoLibrary.CONFIGURE_DELEGATION_TRANSACTION)

        if (output == null) {
            _errorLiveData.value = Event(R.string.app_error_lib)
            _waitingLiveData.value = false
        } else {
            viewModelScope.launch {
                submitTransfer(output)
            }
        }
    }

    private fun submitTransfer(transfer: CreateTransferOutput) {
        _waitingLiveData.value = true
        submitCredentialRequest?.dispose()
        submitCredentialRequest = proxyRepository.submitTransfer(transfer,
            {
                Log.d("Success:"+it)
                _transactionSuccessLiveData.value = true
                _waitingLiveData.value = false
                // Do not disable waiting state yet
            },
            {
                _waitingLiveData.value = false
                it.printStackTrace()
                handleBackendError(it)
            }
        )
    }

    fun setAmount(amount: Long?) {
        delegationData.amount = amount
        loadTransactionFee()
    }
}
