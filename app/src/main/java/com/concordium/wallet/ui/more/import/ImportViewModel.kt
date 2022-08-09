package com.concordium.wallet.ui.more.import

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.security.EncryptionException
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.export.AccountExport
import com.concordium.wallet.data.export.EncryptedExportData
import com.concordium.wallet.data.export.ExportData
import com.concordium.wallet.data.export.IdentityExport
import com.concordium.wallet.data.model.IdentityAttribute
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.model.ShieldedAccountEncryptionStatus
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.data.util.ExportEncryptionHelper
import com.concordium.wallet.util.Log
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.launch
import java.io.IOException
import javax.crypto.Cipher
import javax.crypto.SecretKey

class ImportViewModel(application: Application) :
    AndroidViewModel(application) {

    private val identityProviderRepository = IdentityProviderRepository()
    private val proxyRepository = ProxyRepository()
    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository
    private val recipientRepository: RecipientRepository

    private val gson = App.appCore.gson

    private var encryptedExportData: EncryptedExportData? = null
    private var importPassword: String? = null
    var importResult = ImportResult()


    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _errorAndFinishLiveData = MutableLiveData<Event<Int>>()
    val errorAndFinishLiveData: LiveData<Event<Int>>
        get() = _errorAndFinishLiveData

    private val _showImportPasswordLiveData = MutableLiveData<Event<Boolean>>()
    val showImportPasswordLiveData: LiveData<Event<Boolean>>
        get() = _showImportPasswordLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    private val _showImportConfirmedLiveData = MutableLiveData<Event<Boolean>>()
    val showImportConfirmedLiveData: LiveData<Event<Boolean>>
        get() = _showImportConfirmedLiveData

    private val _finishScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishScreenLiveData

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
        val recipientDao = WalletDatabase.getDatabase(application).recipientDao()
        recipientRepository = RecipientRepository(recipientDao)
    }

    fun initialize() {

    }

    fun checkPasswordRequirements(password: String): Boolean {
        return (password.length >= 6)
    }

    fun handleImportFile(importFile: ImportFile) = viewModelScope.launch {
        val fileContent: String
        try {
            fileContent = importFile.getContentAsString(getApplication())
        } catch (e: IOException) {
            Log.e("Could not read file")
            _errorAndFinishLiveData.value = Event(R.string.import_error_file)
            _waitingLiveData.value = false
            return@launch
        }
        try {
            val encryptedExportData = gson.fromJson(fileContent, EncryptedExportData::class.java)
            if (encryptedExportData.hasRequiredData()) {
                this@ImportViewModel.encryptedExportData = encryptedExportData
                _showImportPasswordLiveData.value = Event(true)
            } else {
                _errorAndFinishLiveData.value = Event(R.string.app_error_json)
            }
        } catch (e: Exception) {
            _errorAndFinishLiveData.value = Event(R.string.app_error_json)
        }
        _waitingLiveData.value = false
    }

    fun startImport(password: String) {
        importPassword = password
        _showAuthenticationLiveData.value = Event(true)
    }

    fun shouldUseBiometrics(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().useBiometrics()
    }

    fun getCipherForBiometrics(): Cipher? {
        return try {
            val cipher = App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForDecryption()
            if (cipher == null) {
                _errorLiveData.value = Event(R.string.app_error_keystore_key_invalidated)
            }
            cipher
        } catch (e: KeystoreEncryptionException) {
            _errorLiveData.value = Event(R.string.app_error_keystore)
            null
        }
    }

    fun checkLogin(password: String) = viewModelScope.launch {
        _waitingLiveData.value = true
        handleAuthPassword(password)
    }

    fun checkLogin(cipher: Cipher) = viewModelScope.launch {
        _waitingLiveData.value = true
        val password = App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(cipher)
        if (password != null) {
            handleAuthPassword(password)
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
        }
    }

    private suspend fun handleAuthPassword(password: String) {
        // Decrypt the private data
        val key = App.appCore.getCurrentAuthenticationManager().derivePasswordKeyInBackground(password)
        if (key == null) {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
            return
        }
        performImport(encryptedExportData, importPassword, key)
    }

    private suspend fun performImport(
        encryptedExportData: EncryptedExportData?,
        importPassword: String?,
        secretKey: SecretKey
    ) {
        if (encryptedExportData == null || importPassword == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            return
        }
        val exportData: ExportData?
        try {
            val decryptedImportData = ExportEncryptionHelper.decryptExportData(importPassword, encryptedExportData)
            exportData = gson.fromJson(decryptedImportData, ExportData::class.java)
        }
        catch (e: IllegalArgumentException) {
            _waitingLiveData.value = false
            _errorAndFinishLiveData.value = Event(R.string.import_error_password_or_file_content)
            return
        }
        catch (e: Exception) {
            _waitingLiveData.value = false
            when (e) {
                is JsonIOException,
                is JsonSyntaxException -> {
                    Log.e("Unexpected json format")
                    _errorAndFinishLiveData.value = Event(R.string.import_error_password_or_file_content)
                }
                is EncryptionException -> {
                    Log.e("Unexpected encryption/decryption error")
                    _errorAndFinishLiveData.value = Event(R.string.import_error_password_or_file_content)
                }
                else -> throw e
            }
            return
        }
        if (exportData == null || !exportData.hasRequiredData()) {
            _waitingLiveData.value = false
            _errorAndFinishLiveData.value = Event(R.string.app_error_json)
            return
        }

        if (!exportData.hasRequiredIdentities()) {
            _waitingLiveData.value = false
            _errorAndFinishLiveData.value = Event(R.string.app_import_missing_identities)
            return
        }

        if (BuildConfig.EXPORT_CHAIN != exportData.environment) {
            _waitingLiveData.value = false
            _errorAndFinishLiveData.value = Event(R.string.app_error_wrong_environment)
            return
        }


        val exportValue = exportData.value
        importResult = ImportResult()
        // Recipients
        val existingRecipientList = recipientRepository.getAll()
        val recipientList = mutableListOf<Recipient>()
        for (recipientExport in exportValue.recipients) {
            try {
                val recipient = Recipient(0, recipientExport.name, recipientExport.address)
                val isDuplicate = existingRecipientList.any { existingRecipient ->
                    existingRecipient.name == recipient.name && existingRecipient.address == recipient.address
                }
                if (!isDuplicate) {
                    recipientList.add(recipient)
                }
                importResult.addRecipientResult(
                    ImportResult.RecipientImportResult(
                        recipient.name,
                        if (isDuplicate) ImportResult.Status.Duplicate else ImportResult.Status.Ok
                    )
                )
            } catch (e: Exception) {
                // In case the mandatory fields are not present
                importResult.addRecipientResult(
                    ImportResult.RecipientImportResult(
                        "",
                        ImportResult.Status.Failed
                    )
                )
            }
        }
        recipientRepository.insertAll(recipientList)
        // Identities and accounts
        val existingIdentityList = identityRepository.getAll()
        val existingAccountList = accountRepository.getAll()
        for (identityExport in exportValue.identities) {
            var identity: Identity? = null
            var identityId: Long? = null
            var identityImportResult = ImportResult.IdentityImportResult("", ImportResult.Status.Failed)
            try {
                identity = mapIdentityFromExport(identityExport)
            } catch (e: Exception) {
                // In case the mandatory fields are not present
                // Setting state is handled below
            }
            if (identity == null) {
                importResult.addIdentityResult(identityImportResult)
            } else {
                // Find existing/duplicate Identity (based on idCredPub)
                val oldIdentity = existingIdentityList.firstOrNull { existingIdentity ->
                    // Avoid that two identities without an IdentityObject gets matched
                    !TextUtils.isEmpty(existingIdentity.identityObject?.preIdentityObject?.pubInfoForIp?.idCredPub) &&
                            existingIdentity.identityObject?.preIdentityObject?.pubInfoForIp?.idCredPub == identity.identityObject?.preIdentityObject?.pubInfoForIp?.idCredPub
                }
                // Only save the imported identity, if it is not a duplicate
                identityId = oldIdentity?.id?.toLong() ?: identityRepository.insert(identity)
                val status = if (oldIdentity == null) ImportResult.Status.Ok else ImportResult.Status.Duplicate
                identityImportResult = ImportResult.IdentityImportResult(identity.name, status)
                importResult.addIdentityResult(identityImportResult)
            }
            // Accounts
            val accountList = mutableListOf<Account>()
            if (hasAccountList(identityExport)) {
                for (accountExport in identityExport.accounts) {
                    var account: Account? = null
                    if (identityId != null) {
                        try {
                            account = encryptAndMapAccount(accountExport, identityId, secretKey)
                        } catch (e: Exception) {
                            // In case the mandatory fields are not present
                            // Setting state is handled below
                        }
                    }
                    if (account == null) {
                        val accountImportResult = ImportResult.AccountImportResult("", ImportResult.Status.Failed)
                        identityImportResult.addAccountResult(accountImportResult)
                    } else {
                        var status = ImportResult.Status.Ok
                        val existingDuplicate = existingAccountList.firstOrNull { existingAccount -> existingAccount.address == account.address }
                        if (existingDuplicate == null) {
                            accountList.add(account)
                        } else {
                            // Replace with imported account if the existing is readonly - update the fields that are part of the AccountExport
                            if (existingDuplicate.readOnly) {
                                existingDuplicate.readOnly = false
                                existingDuplicate.name = account.name
                                existingDuplicate.submissionId = account.submissionId
                                existingDuplicate.encryptedAccountData = account.encryptedAccountData
                                existingDuplicate.revealedAttributes = account.revealedAttributes
                                existingDuplicate.credential = account.credential
                                accountRepository.update(existingDuplicate)
                            } else {
                                status = ImportResult.Status.Duplicate
                            }
                        }
                        val accountImportResult = ImportResult.AccountImportResult(account.name, status)
                        identityImportResult.addAccountResult(accountImportResult)
                    }
                }
                accountRepository.insertAll(accountList)
            }
            // Read-only accounts - even though there are no accounts in the import file, there can be accounts from other devices
            // The account list used to check for existing account must include the ones that was just added for this identity
            if (identityId != null) {
                accountList.addAll(existingAccountList)
                //handleReadOnlyAccounts(accountList, identityExport, identityId, identityImportResult)
            }
        } // End of identity
        confirmImport()
        _waitingLiveData.value = false
    }
/*
    private suspend fun handleReadOnlyAccounts(
        existingAccountList: List<Account>,
        identityExport: IdentityExport,
        identityId: Long,
        identityImportResult: ImportResult.IdentityImportResult
    ) {
        val globalParams: GlobalParams?
        try {
            val globalParamsWrapper = identityProviderRepository.getGlobalInfoSuspended()
            globalParams = globalParamsWrapper.value
        } catch (e: Exception) {
            return
        }
        val generateAccountsInput = GenerateAccountsInputV1(globalParams, identityExport.identityObject)
        //val possibleAccountList = App.appCore.cryptoLibrary.generateAccounts(generateAccountsInput) ?: return
        //Log.d("Generated account info for ${possibleAccountList.size} accounts")
        //checkExistingAccountsForReadOnly(possibleAccountList, 0, existingAccountList, identityId, identityImportResult)
    }
*/
    /*
    private suspend fun checkExistingAccountsForReadOnly(
        possibleAccountList: List<PossibleAccount>,
        startIndex: Int,
        existingAccountList: List<Account>,
        identityId: Long,
        identityImportResult: ImportResult.IdentityImportResult
    ) {
        val readOnlyAccountList = mutableListOf<Account>()
        val shouldContinue = true
        var index = startIndex
        while (index < possibleAccountList.size && shouldContinue) {
            try {
                val possibleAccount = possibleAccountList[index]
                val accountBalance = proxyRepository.getAccountBalanceSuspended(possibleAccount.accountAddress)
                Log.d("Got account balance for index $index and accountAddress ${possibleAccount.accountAddress} accountBalance $accountBalance")
                if (accountBalance.accountExists()) {
                    // Check if the account exists
                    val isAccountDuplicate = existingAccountList.any { existingAccount -> existingAccount.address == possibleAccount.accountAddress }
                    if (!isAccountDuplicate) {
                        readOnlyAccountList.add(createReadOnlyAccount(possibleAccount, identityId))
                        val name = Formatter.formatAsFirstEight(possibleAccount.accountAddress)
                        identityImportResult.addReadOnlyAccountResult(ImportResult.AccountImportResult(name, ImportResult.Status.Ok))
                    }
                } else {
                    Log.d("Unused account address - no need to continue")
                }
                index++
            } catch (e: Exception) {
                val ex = BackendErrorHandler.getCoroutineBackendException(e)
                if (ex != null && ex is BackendErrorException) {
                    Log.d("Backend error - unused account address", ex)
                } else {
                    // Other exceptions like connection problems - just continue
                    Log.e("Unexpected exception when getting account balance", e)
                    index++
                }
            }
        }
        accountRepository.insertAll(readOnlyAccountList)
        recipientRepository.insertAll(readOnlyAccountList.map { Recipient(0, name = it.address.substring(0,8), address = it.address) }.toMutableList())
    }
*/
    @Suppress("SENSELESS_COMPARISON")
    private fun hasAccountList(identityExport: IdentityExport): Boolean {
        // Because the data is parsed by Gson, the non-nullable members can actually be null
        return identityExport.accounts != null
    }

    private suspend fun encryptAndMapAccount(
        accountExport: AccountExport,
        identityId: Long,
        secretKey: SecretKey
    ): Account? {
        val accountDataJson = gson.toJson(StorageAccountData(accountExport.address, accountExport.accountKeys, accountExport.encryptionSecretKey, accountExport.commitmentsRandomness))
        val accountDataEncrypted = App.appCore.getCurrentAuthenticationManager().encryptInBackground(secretKey, accountDataJson)
        return if (accountDataEncrypted == null) {
            Log.e("Could not encrypt accountData for account: ${accountExport.name}")
            null
        } else {
            mapAccountFromExport(accountExport, identityId, accountDataEncrypted)
        }
    }

    private fun mapIdentityFromExport(identityExport: IdentityExport): Identity {
        return Identity(
            0,
            identityExport.name,
            IdentityStatus.DONE,
            "",
            "",
            identityExport.nextAccountNumber,
            identityExport.identityProvider,
            identityExport.identityObject,
            ""
        )
    }

    private fun mapAccountFromExport(
        accountExport: AccountExport,
        identityId: Long,
        accountDataEncrypted: String
    ): Account {
        return Account(
            0,
            identityId.toInt(),
            accountExport.name,
            accountExport.address,
            accountExport.submissionId,
            TransactionStatus.FINALIZED,
            accountDataEncrypted,
            mapRevealedAttributes(accountExport.revealedAttributes),
            accountExport.credential,
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
            null
        )
    }
/*
    private fun createReadOnlyAccount(
        possibleAccount: PossibleAccount,
        identityId: Long
    ): Account {
        return Account(
            0,
            identityId.toInt(),
            "",
            possibleAccount.accountAddress,
            "",
            TransactionStatus.FINALIZED,
            "",
            emptyList(),
            null,
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
            true,
            null

        )
    }*/

    private fun mapRevealedAttributes(map: HashMap<String, String>): List<IdentityAttribute> {
        val list = mutableListOf<IdentityAttribute>()
        for ((k, v) in map) {
            list.add(IdentityAttribute(k, v))
        }
        return list
    }

    private fun confirmImport() {
        App.appCore.session.setAccountsBackedUp(true)
        _showImportConfirmedLiveData.value = Event(true)
    }

    fun finishImport() {
        _finishScreenLiveData.value = Event(true)
    }

    fun usePasscode(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().usePasscode()
    }
}