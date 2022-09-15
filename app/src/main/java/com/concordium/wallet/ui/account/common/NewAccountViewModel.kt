package com.concordium.wallet.ui.account.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.AppConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.backend.BackendErrorException
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateCredentialInputV1
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.account.newaccountidentityattributes.SelectableIdentityAttribute
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.google.gson.JsonArray
import kotlinx.coroutines.launch
import javax.crypto.Cipher

open class NewAccountViewModel(application: Application) : AndroidViewModel(application) {
    private val identityProviderRepository = IdentityProviderRepository()
    private val proxyRepository = ProxyRepository()
    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository
    private val recipientRepository: RecipientRepository
    private val gson = App.appCore.gson

    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null
    private var submitCredentialRequest: BackendRequest<SubmissionData>? = null
    private var accountSubmissionStatusRequest: BackendRequest<AccountSubmissionStatus>? = null
    private var tempData = TempData()

    lateinit var identity: Identity

    private lateinit var accountName: String

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    private val _gotoAccountCreatedLiveData = MutableLiveData<Event<Account>>()
    val gotoAccountCreatedLiveData: LiveData<Event<Account>>
        get() = _gotoAccountCreatedLiveData

    private val _gotoFailedLiveData = MutableLiveData<Event<Pair<Boolean, BackendError?>>>()
    val gotoFailedLiveData: LiveData<Event<Pair<Boolean, BackendError?>>>
        get() = _gotoFailedLiveData


    open fun initialize(identity: Identity) {
        this.identity = identity
    }

    open fun initialize(accountName: String, identity: Identity) {
        this.accountName = accountName
        this.identity = identity
    }

    class TempData {
        var revealedAttributeList: List<SelectableIdentityAttribute> = ArrayList()
        var globalParams: GlobalParams? = null
        var submissionStatus: TransactionStatus? = null
        var submissionId: String? = null
        var accountAddress: String? = null
        var encryptedAccountData: String? = null
        var credential: CredentialWrapper? = null
        var nextCredNumber: Int? = null
    }

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
        val recipientDao = WalletDatabase.getDatabase(application).recipientDao()
        recipientRepository = RecipientRepository(recipientDao)
    }

    override fun onCleared() {
        super.onCleared()
        globalParamsRequest?.dispose()
        submitCredentialRequest?.dispose()
        accountSubmissionStatusRequest?.dispose()
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        if (throwable is BackendErrorException) {
            _gotoFailedLiveData.value = Event(Pair(true, throwable.error))
        } else {
            _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(throwable))
        }
    }

    fun confirmWithoutAttributes() {
        tempData.revealedAttributeList = emptyList()
        getGlobalInfo()
    }

    private fun getGlobalInfo() {
        // Show waiting state for the full flow, but remove it of any errors occur
        _waitingLiveData.value = true
        globalParamsRequest?.dispose()
        globalParamsRequest = identityProviderRepository.getIGlobalInfo(
            {
                tempData.globalParams = it.value
                _showAuthenticationLiveData.value = Event(true)
                _waitingLiveData.value = false
            },
            {
                _waitingLiveData.value = false
                handleBackendError(it)
            }
        )
    }

    fun shouldUseBiometrics(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().useBiometrics()
    }

    fun getCipherForBiometrics(): Cipher? {
        return try {
            val cipher = App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForDecryption()
            if (cipher == null) {
                _errorLiveData.value = Event(R.string.app_error_keystore_key_invalidated)
                _waitingLiveData.value = false
            }
            cipher
        } catch (e: KeystoreEncryptionException) {
            _errorLiveData.value = Event(R.string.app_error_keystore)
            _waitingLiveData.value = false
            null
        }
    }

    fun continueWithPassword(password: String) = viewModelScope.launch {
        _waitingLiveData.value = true
        decryptAndContinue(password)
    }

    fun checkLogin(cipher: Cipher) = viewModelScope.launch {
        _waitingLiveData.value = true
        val password = App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(cipher)
        if (password != null) {
            decryptAndContinue(password)
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
        }
    }

    private suspend fun decryptAndContinue(password: String) {
        val globalParams = tempData.globalParams
        if (globalParams == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return
        }
        createCredentials(password, globalParams)
    }

    private suspend fun createCredentials(password: String, globalParams: GlobalParams) {
        val identityProvider = identity.identityProvider
        val identityObject = identity.identityObject
        val idProviderInfo = identityProvider.ipInfo
        val info = identityProvider.arsInfos

        if (identityObject == null) {
            Log.e("Identity is not ready to use for account creation (no identityObject")
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return
        }

        tempData.nextCredNumber = accountRepository.nextCredNumber(identity.id)

        val revealedAttributes = JsonArray()
        for (identityAttribute in tempData.revealedAttributeList) {
            revealedAttributes.add(identityAttribute.name)
        }

        val net = AppConfig.net
        val seed = AuthPreferences(getApplication()).getSeedPhrase()

        val credentialInput = CreateCredentialInputV1(
            idProviderInfo,
            info,
            globalParams,
            identityObject,
            revealedAttributes,
            seed,
            net,
            identity.identityIndex,
            tempData.nextCredNumber ?: 0,
            (DateTimeUtil.nowPlusMinutes(5).time) / 1000
        )

        val output = App.appCore.cryptoLibrary.createCredentialV1(credentialInput)
        if (output == null) {
            _errorLiveData.value = Event(R.string.app_error_lib)
            _waitingLiveData.value = false
        } else {
            tempData.accountAddress = output.accountAddress
            val jsonToBeEncrypted = gson.toJson(StorageAccountData(output.accountAddress, output.accountKeys, output.encryptionSecretKey, output.commitmentsRandomness))

            val storageAccountDataEncrypted = App.appCore.getCurrentAuthenticationManager().encryptInBackground(password, jsonToBeEncrypted)
            if (storageAccountDataEncrypted != null) {
                tempData.encryptedAccountData = storageAccountDataEncrypted
                tempData.credential = output.credential
                submitCredential(output.credential)
            } else {
                _errorLiveData.value = Event(R.string.app_error_encryption)
                _waitingLiveData.value = false
            }
        }
    }

    private fun submitCredential(credentialWrapper: CredentialWrapper) {
        _waitingLiveData.value = true
        submitCredentialRequest?.dispose()
        submitCredentialRequest = proxyRepository.submitCredential(credentialWrapper,
            {
                tempData.submissionId = it.submissionId
                submissionStatus(it.submissionId)
                // Do not disable waiting state yet
            },
            {
                _waitingLiveData.value = false
                handleBackendError(it)
            }
        )
    }

    private fun submissionStatus(submissionId: String) {
        _waitingLiveData.value = true
        accountSubmissionStatusRequest?.dispose()
        accountSubmissionStatusRequest = proxyRepository.getAccountSubmissionStatus(submissionId,
            {
                tempData.submissionStatus = it.status
                finishAccountCreation()
                // Do not disable waiting state yet
            },
            {
                _waitingLiveData.value = false
                handleBackendError(it)
            }
        )
    }

    private fun finishAccountCreation() {
        val accountAddress = tempData.accountAddress
        val submissionId = tempData.submissionId
        val encryptedAccountData = tempData.encryptedAccountData
        val credential = tempData.credential
        val submissionStatus = tempData.submissionStatus
        val revealedAttributeList = tempData.revealedAttributeList
        if (accountAddress == null || submissionId == null || encryptedAccountData == null ||
            credential == null || submissionStatus == null
        ) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return
        }

        val newAccount = Account(
            0,
            identity.id,
            accountName,
            accountAddress,
            submissionId,
            submissionStatus,
            encryptedAccountData,
            revealedAttributeList.map { IdentityAttribute(it.name, it.value) },
            credential,
            0,
            0,
            0,
            0,
            0,
            null,
            null,
            ShieldedAccountEncryptionStatus.ENCRYPTED,
            0,
            0,
            false,
            null,
            null,
            null,
            null,
            null,
            tempData.nextCredNumber ?: 0
        )

        saveNewAccount(newAccount)
    }

    private fun saveNewAccount(account: Account) = viewModelScope.launch {
        account.name = Account.getDefaultName(account.address)
        val accountId = accountRepository.insert(account)
        identityRepository.update(identity)
        account.id = accountId.toInt()
        _gotoAccountCreatedLiveData.value = Event(account)
        _waitingLiveData.value = false
    }

    fun usePasscode(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().usePasscode()
    }
}