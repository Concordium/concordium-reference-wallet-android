package com.concordium.wallet.ui.more.alterpassword

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.crypto.SecretKey

class AlterPasswordViewModel(application: Application) :
    AndroidViewModel(application) {

    private var initialDecryptedAccountsList: ArrayList<Account> = ArrayList()
    private var initialDecryptedIdentityList: List<Identity> = emptyList()

    private var decryptedSeed: String? = null

    private var database: WalletDatabase

    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository

    val usePasscode = App.appCore.getCurrentAuthenticationManager().usePasscode()

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _checkAccountsIdentitiesDoneLiveData = MutableLiveData<Boolean>()
    val checkAccountsIdentitiesDoneLiveData: LiveData<Boolean>
        get() = _checkAccountsIdentitiesDoneLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    //Final encryption flow
    private val _doneFinalChangePasswordLiveData = MutableLiveData<Event<Boolean>>()
    val doneFinalChangePasswordLiveData: LiveData<Event<Boolean>>
        get() = _doneFinalChangePasswordLiveData

    private val _errorFinalChangePasswordLiveData = MutableLiveData<Event<Boolean>>()
    val errorFinalChangePasswordLiveData: LiveData<Event<Boolean>>
        get() = _errorFinalChangePasswordLiveData


    //Initial decryption flow
    private val _doneInitialAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val doneInitialAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _doneInitialAuthenticationLiveData

    private val _errorInitialAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val errorInitialAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _errorInitialAuthenticationLiveData


    init {
        database = WalletDatabase.getDatabase(application)
        val identityDao = database.identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = database.accountDao()
        accountRepository = AccountRepository(accountDao)
    }

    fun initialize() {

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
        decryptOriginalAndContinue(key)
    }

    fun finishPasswordChange(newPassword: String) {
        viewModelScope.launch {
            val newDecryptKey = App.appCore.getCurrentAuthenticationManager().derivePasswordKeyInBackground(newPassword)
            if (newDecryptKey == null) {
                _errorLiveData.value = Event(R.string.app_error_encryption)
                _waitingLiveData.value = false
            }
            else{
                encryptAndFinalize(newDecryptKey)
            }
        }
    }

    private fun decryptOriginalAndContinue(decryptKey: SecretKey) =
        viewModelScope.launch {
            _waitingLiveData.value = true
            var allSuccess = true
            try {
                initialDecryptedIdentityList = ArrayList()
                initialDecryptedIdentityList = identityRepository.getAllDone()
                for (identity in initialDecryptedIdentityList) {
                    val tmpInitialDecryptedAccountsList = accountRepository.getAllByIdentityId(identity.id)
                    for (account in tmpInitialDecryptedAccountsList) {
                        if (account.encryptedAccountData.isNotEmpty()) {
                            val accountDataDecrypted = App.appCore.getOriginalAuthenticationManager().decryptInBackground(decryptKey, account.encryptedAccountData)
                            if(accountDataDecrypted != null && isJson(accountDataDecrypted)){
                                account.encryptedAccountData = accountDataDecrypted
                            }
                            else{
                                allSuccess = false
                            }
                        }
                        initialDecryptedAccountsList.add(account)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                allSuccess = false
            }

            try {
                decryptedSeed = AuthPreferences(getApplication()).getSeedPhrase(decryptKey)
                if(decryptedSeed == null){
                    allSuccess = false
                }
            }catch (e: Exception) {
                e.printStackTrace()
                allSuccess = false
            }

            if(allSuccess){
                App.appCore.startResetAuthFlow()
                _doneInitialAuthenticationLiveData.value = Event(true)
                _waitingLiveData.value = false
            }
            else{
                _errorInitialAuthenticationLiveData.value = Event(true)
                _waitingLiveData.value = false
            }
        }


    private fun encryptAndFinalize(encryptKey: SecretKey) =
        viewModelScope.launch {
            _waitingLiveData.value = true

            var allSuccess = true

            if(decryptedSeed != null){
                try {
                    val seedPhraseEncrypted = App.appCore.getOriginalAuthenticationManager()
                        .encryptInBackground(encryptKey, decryptedSeed!!)
                    if (seedPhraseEncrypted == null || !AuthPreferences(getApplication()).updateEncryptedSeedPhrase(seedPhraseEncrypted)) {
                            allSuccess = false
                    }
                    decryptedSeed = null
                }catch (e: Exception){
                    allSuccess = false
                }
            }else{
                allSuccess = false
            }

            database.withTransaction {
                try {
                    for (account in initialDecryptedAccountsList) {
                        if (account.encryptedAccountData.isNotEmpty()) {
                            val accountDataEncrypted = App.appCore.getOriginalAuthenticationManager().encryptInBackground(encryptKey, account.encryptedAccountData)//Which is decrypted by now!
                            if (accountDataEncrypted != null) {
                                account.encryptedAccountData = accountDataEncrypted
                                accountRepository.update(account)
                            }
                            else {
                                allSuccess = false
                            }
                        }
                    }

                    initialDecryptedAccountsList = ArrayList()
                    initialDecryptedIdentityList = emptyList()
                } catch (e: Exception) {
                    allSuccess = false
                }

                if(allSuccess){
                    App.appCore.finalizeResetAuthFlow()
                    viewModelScope.launch {
                        _doneFinalChangePasswordLiveData.value = Event(true)
                    }
                }
                else{
                    App.appCore.cancelResetAuthFlow()
                    _errorFinalChangePasswordLiveData.value = Event(true)
                    throw Exception()
                }
            }
        }

    private fun isJson(Json: String?): Boolean {
        val gson = Gson()
        return try {
            gson.fromJson(Json, Any::class.java)
            val jsonObjType: Any = gson.fromJson(Json, Any::class.java).javaClass
            jsonObjType != String::class.java
        } catch (ex: JsonSyntaxException) {
            false
        }
    }

    fun checkAndStartPasscodeChange() = viewModelScope.launch {
        _checkAccountsIdentitiesDoneLiveData.value = (identityRepository.getNonDoneCount() + accountRepository.getNonDoneCount()) == 0
    }
}
