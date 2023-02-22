package com.concordium.wallet.ui.passphrase.recover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.ui.passphrase.common.WordsPickedBaseListAdapter
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toHex
import kotlinx.coroutines.launch
import java.util.*

class PassPhraseRecoverViewModel(application: Application) : AndroidViewModel(application) {
    var wordsPicked = arrayOfNulls<String>(WORD_COUNT + (WordsPickedBaseListAdapter.OFFSET * 2) + 1)
    var allWords = listOf<String>()

    companion object {
        val WORD_COUNT: Int = Mnemonics.WordCount.COUNT_24.count
    }

    private val _saveSeedLiveData = MutableLiveData<Boolean>()
    val saveSeed: LiveData<Boolean>
        get() = _saveSeedLiveData

    private val _validateLiveData = MutableLiveData<Boolean>()
    val validate: LiveData<Boolean>
        get() = _validateLiveData

    private val _seedLiveData = MutableLiveData<String>()
    val seed: LiveData<String>
        get() = _seedLiveData

    fun loadAllWords() {
        allWords = Mnemonics.getCachedWords(Locale.ENGLISH.language)
    }

    fun clearWordsPicked() {
        wordsPicked = arrayOfNulls(wordsPicked.size)
    }

    fun setPredefinedPhraseForTesting(password: String) = viewModelScope.launch  {
        if (BuildConfig.DEBUG) {
            //AuthPreferences(getApplication()).setSeedPhrase("health smoke abandon middle outer method meadow sorry whale random cupboard thank album exclude idle month exit quarter shell portion eternal legal rent tonight") // testnet CBW-320
            val saveSuccess = AuthPreferences(getApplication()).tryToSetEncryptedSeedPhrase(
                "nothing ill myself guitar antique demise awake twelve fall victory grow segment bus puppy iron vicious skate piece tobacco stable police plunge coin fee",
                password
            )// testnet
            _saveSeedLiveData.value = saveSuccess
            _validateLiveData.value = saveSuccess

        }
    }

    fun validateInputCode() {
        var success = false
        val entered = wordsPicked.filterNotNull().filter { it != WordsPickedBaseListAdapter.BLANK }
        if (entered.size < WORD_COUNT)
            return
        if (entered.size == WORD_COUNT) {
            val enteredPhrase: String = entered.joinToString(" ") { it }
            try {
                val result = Mnemonics.MnemonicCode(enteredPhrase).toSeed()
                success = result.isNotEmpty() && result.size == 64 && result.toHex().length == 128
                if (success) {
                    _seedLiveData.value = enteredPhrase
                }
            } catch (ex: Exception) {
                Log.d(ex.message ?: "")
            }
        }
        _validateLiveData.value = success
    }

    fun setSeedPhrase(seed: String, password: String) = viewModelScope.launch {
        _saveSeedLiveData.value = AuthPreferences(getApplication()).tryToSetEncryptedSeedPhrase(
            seed,
            password
        )
    }
}
