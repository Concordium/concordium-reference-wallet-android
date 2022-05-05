package com.concordium.wallet.ui.more.export

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.DataFileProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.export.*
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.data.util.ExportEncryptionHelper
import com.concordium.wallet.data.util.FileUtil
import com.concordium.wallet.util.Log
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import javax.crypto.Cipher
import javax.crypto.SecretKey

class ExportViewModel(application: Application) :
    AndroidViewModel(application) {

    companion object {
        private const val FILE_NAME = "concordium-backup.concordiumwallet"
    }

    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository
    private val recipientRepository: RecipientRepository

    private val gson = App.appCore.gson
    private var _exportPassword: String = ""

    var decryptKey: SecretKey? = null

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _errorPasswordLiveData = MutableLiveData<Event<Boolean>>()
    val errorPasswordLiveData: LiveData<Event<Boolean>>
        get() = _errorPasswordLiveData

    private val _errorExportLiveData = MutableLiveData<Event<List<String>>>()
    val errorExportLiveData: LiveData<Event<List<String>>>
        get() = _errorExportLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    private val _showRequestPasswordLiveData = MutableLiveData<Event<Boolean>>()
    val showRequestPasswordLiveData: LiveData<Event<Boolean>>
        get() = _showRequestPasswordLiveData

    private val _shareExportFileLiveData = MutableLiveData<Event<Boolean>>()
    val shareExportFileLiveData: LiveData<Event<Boolean>>
        get() = _shareExportFileLiveData

    private val _showRepeatPasswordScreenLiveData = MutableLiveData<Event<Boolean>>()
    val showRepeatPasswordScreenLiveData: LiveData<Event<Boolean>>
        get() = _showRepeatPasswordScreenLiveData

    private val _finishPasswordScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishPasswordScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishPasswordScreenLiveData

    private val _finishRepeatPasswordScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishRepeatPasswordScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishRepeatPasswordScreenLiveData

    private val _errorNonIdenticalRepeatPasswordLiveData = MutableLiveData<Event<Boolean>>()
    val errorNonIdenticalRepeatPasswordLiveData: LiveData<Event<Boolean>>
        get() = _errorNonIdenticalRepeatPasswordLiveData

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

    fun export(force: Boolean) {
        viewModelScope.launch {
            val accountNamesFailed = mutableListOf<String>()
            val identityList = identityRepository.getAllDone()
            for (identity in identityList) {
                val accountList = accountRepository.getAllByIdentityId(identity.id)
                for (account in accountList) {
                    if(account.transactionStatus != TransactionStatus.FINALIZED){
                        accountNamesFailed.add(account.name)
                    }
                }
            }

            if(!force && accountNamesFailed.size>0){
                _errorExportLiveData.value = Event(accountNamesFailed)
            }
            else{
                _showAuthenticationLiveData.value = Event(true)
            }
        }
    }

    fun shouldUseBiometrics(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().useBiometrics()
    }

    fun getCipherForBiometrics(): Cipher? {
        try {
            val cipher = App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForDecryption()
            if (cipher == null) {
                _errorLiveData.value = Event(R.string.app_error_keystore_key_invalidated)
            }
            return cipher
        } catch (e: KeystoreEncryptionException) {
            _errorLiveData.value = Event(R.string.app_error_keystore)
            return null
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
        decryptKey = key

        _showRequestPasswordLiveData.value = Event(true)
        _waitingLiveData.value = false
    }

    fun getEncryptedFileWithPath() = Uri.parse("content://" + DataFileProvider.AUTHORITY + File.separator.toString() + FILE_NAME)

    fun finalizeEncryptionOfFile() {
        decryptKey?.let { decryptAndContinue(it, _exportPassword) }
        ?: run { _errorLiveData.value = Event(R.string.app_error_general) }
    }

    fun saveFileToLocalFolder(destinationUri: Uri) {
        decryptKey?.let { saveFileToLocalFolder(it, _exportPassword, destinationUri) }
            ?: run { _errorLiveData.value = Event(R.string.app_error_general) }
    }

    private fun saveFileToLocalFolder(decryptKey: SecretKey, exportPassword: String, destinationUri: Uri) =
        viewModelScope.launch {
            _waitingLiveData.value = true
            try {
                val fileContent = createExportFileContent(decryptKey, exportPassword)
                FileUtil.writeFile(destinationUri, FILE_NAME, fileContent)
                App.appCore.session.setAccountsBackedUp(true)
                _errorLiveData.value = Event(R.string.export_backup_saved_local)
                _waitingLiveData.value = false
            } catch (e: Exception) {
                _waitingLiveData.value = false
                when (e) {
                    is JsonIOException,
                    is JsonSyntaxException -> {
                        _errorLiveData.value = Event(R.string.app_error_json)
                    }
                    is FileNotFoundException -> {
                        _errorLiveData.value = Event(R.string.export_error_file)
                    }
                    else -> throw e
                }
            }
        }

    private fun decryptAndContinue(decryptKey: SecretKey, exportPassword: String) =
        viewModelScope.launch {
            _waitingLiveData.value = true
            try {
                val fileContent = createExportFileContent(decryptKey, exportPassword)
                FileUtil.saveFile(App.appContext, FILE_NAME, fileContent)
                _shareExportFileLiveData.value = Event(true)
                _waitingLiveData.value = false
            } catch (e: Exception) {
                _waitingLiveData.value = false
                when (e) {
                    is JsonIOException,
                    is JsonSyntaxException -> {
                        _errorLiveData.value = Event(R.string.app_error_json)
                    }
                    is FileNotFoundException -> {
                        _errorLiveData.value = Event(R.string.export_error_file)
                    }
                    else -> throw e
                }
            }
        }

    private suspend fun createExportFileContent(decryptKey: SecretKey, exportPassword: String) : String {
        // Get all data, parse, decrypt and generate file content
        val identityExportList = mutableListOf<IdentityExport>()
        val failedAccountAddressExportList = mutableListOf<String>()
        val identityList = identityRepository.getAllDone()
        for (identity in identityList) {
            val accountExportList = mutableListOf<AccountExport>()
            val accountList = accountRepository.getAllByIdentityId(identity.id)
            for (account in accountList) {
                if (account.readOnly){
                    // Skip read only accounts (they will be found when importing)
                    continue
                }
                if(account.transactionStatus == TransactionStatus.FINALIZED){
                    val accountDataDecryped =
                        App.appCore.getCurrentAuthenticationManager().decryptInBackground(
                            decryptKey,
                            account.encryptedAccountData
                        )
                    val accountData =
                        gson.fromJson(accountDataDecryped, StorageAccountData::class.java)
                    val accountExport = mapAccountToExport(account, accountData)
                    accountExportList.add(accountExport)
                }
                else{
                    failedAccountAddressExportList.add(account.address)
                }
            }
            val privateIdObjectDataDecryped =
                App.appCore.getCurrentAuthenticationManager().decryptInBackground(
                    decryptKey,
                    identity.privateIdObjectDataEncrypted
                )
            val privateIdObjectData =
                gson.fromJson(privateIdObjectDataDecryped, RawJson::class.java)
            val identityExport =
                mapIdentityToExport(identity, privateIdObjectData, accountExportList)
            identityExportList.add(identityExport)
        }
        val recipientExportList = mutableListOf<RecipientExport>()
        val recipientList = recipientRepository.getAll()
        for (recipient in recipientList) {
            if(!failedAccountAddressExportList.contains(recipient.address)){
                recipientExportList.add(RecipientExport(recipient.name, recipient.address))
            }
        }
        val exportValue = ExportValue(identityExportList, recipientExportList)
        val exportData = ExportData("concordium-mobile-wallet-data", 1, exportValue, BuildConfig.EXPORT_CHAIN)
        val jsonOutput = gson.toJson(exportData)
        Log.d("ExportData: $jsonOutput")

        // Encrypt
        val encryptedExportData = ExportEncryptionHelper.encryptExportData(exportPassword, jsonOutput)
        val fileContent = gson.toJson(encryptedExportData)

        return fileContent
    }

    private fun mapIdentityToExport(
        identity: Identity,
        privateIdObjectData: RawJson,
        accountList: List<AccountExport>
    ): IdentityExport {
        return IdentityExport(
            identity.name,
            identity.nextAccountNumber,
            identity.identityProvider,
            identity.identityObject!!,
            privateIdObjectData,
            accountList
        )
    }

    private fun mapAccountToExport(account: Account, accountData: StorageAccountData): AccountExport {
        return AccountExport(
            account.name,
            account.address,
            account.submissionId,
            accountData.accountKeys,
            accountData.commitmentsRandomness,
            mapRevealedAttributes(account.revealedAttributes),
            account.credential ?: CredentialWrapper(RawJson("{}"), 0),
            accountData.encryptionSecretKey
        )
    }

    private fun mapRevealedAttributes(list: List<IdentityAttribute>): HashMap<String, String> {
        val map = HashMap<String, String>()
        for (item in list) {
            map[item.name] = item.value
        }
        return map
    }

    fun checkPasswordRequirements(password: String): Boolean {
        return (password.length >= 6)
    }

    fun setStartExportPassword(password: String) {
        _exportPassword = password
        _showRepeatPasswordScreenLiveData.value = Event(true)
    }

    fun checkExportPassword(password: String) {
        val isEqual = _exportPassword.equals(password)
        if (!isEqual) {
            _errorNonIdenticalRepeatPasswordLiveData.value = Event(false)
        }
        _finishRepeatPasswordScreenLiveData.value = Event(isEqual)
    }

    fun usePasscode(): Boolean {
        return App.appCore.getCurrentAuthenticationManager().usePasscode()
    }
}