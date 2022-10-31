package com.concordium.wallet.ui.more.export

import android.app.Application
import android.net.Uri
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.FileUtil
import com.google.gson.Gson
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class ExportAccountKeysViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var account: Account
    lateinit var accountDataKeys: AccountDataKeys

    val textResourceInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val accountData: MutableLiveData<AccountData> by lazy { MutableLiveData<AccountData>() }

    fun saveFileToLocalFolder(destinationUri: Uri) {
        viewModelScope.launch {
            val accountKeys = AccountKeys(accountDataKeys, account.credential?.getThreshold() ?: 1)
            val credentials = Credentials(account.credential?.getCredId() ?: "")
            val value = Value(accountKeys, credentials, account.address)
            val fileContent = Gson().toJson(ExportAccountKeys("concordium-mobile-wallet-account", account.credential?.v ?: 0, BuildConfig.ENV_NAME, value))
            FileUtil.writeFile(destinationUri, "${account.address}.export", fileContent)
            textResourceInt.postValue(R.string.export_account_keys_file_exported)
        }
    }

    fun getCipherForBiometrics(): Cipher? {
        return try {
            val cipher =
                App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForDecryption()
            if (cipher == null) {
                textResourceInt.postValue(R.string.app_error_keystore_key_invalidated)
            }
            cipher
        } catch (e: KeystoreEncryptionException) {
            textResourceInt.postValue(R.string.app_error_keystore)
            null
        }
    }

    fun continueWithPassword(password: String) = viewModelScope.launch {
        decryptAndContinue(password)
    }

    fun checkLogin(cipher: Cipher) = viewModelScope.launch {
        val password = App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(cipher)
        if (password != null) {
            decryptAndContinue(password)
        } else {
            textResourceInt.postValue(R.string.app_error_encryption)
        }
    }

    private suspend fun decryptAndContinue(password: String) {
        val storageAccountDataEncrypted = account.encryptedAccountData
        if (TextUtils.isEmpty(storageAccountDataEncrypted)) {
            textResourceInt.postValue(R.string.app_error_general)
            return
        }
        val decryptedJson = App.appCore.getCurrentAuthenticationManager().decryptInBackground(password, storageAccountDataEncrypted)
        if (decryptedJson != null) {
            val credentialsOutput = App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
            accountData.postValue(credentialsOutput.accountKeys)
        } else {
            textResourceInt.postValue(R.string.app_error_encryption)
        }
    }
}
