package com.concordium.wallet.ui.passphrase.recover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.ui.passphrase.common.WordsPickedBaseListAdapter
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toHex
import java.io.Serializable
import java.util.*

class PassPhraseRecoverViewModel(application: Application) : AndroidViewModel(application), Serializable {
    var wordsPicked = arrayOfNulls<String>(WORD_COUNT + (WordsPickedBaseListAdapter.OFFSET * 2) + 1)
    var allWords = listOf<String>()

    companion object {
        val WORD_COUNT: Int = Mnemonics.WordCount.COUNT_24.count
        const val PASS_PHRASE_RECOVER_DATA = "PASS_PHRASE_RECOVER_DATA"
    }

    private val _validateLiveData = MutableLiveData<Boolean>()
    val validate: LiveData<Boolean>
        get() = _validateLiveData

    fun loadAllWords() {
        allWords = Mnemonics.getCachedWords(Locale.ENGLISH.language)
    }

    fun clearWordsPicked() {
        wordsPicked = arrayOfNulls(wordsPicked.size)
    }

    fun hack() {
        if (BuildConfig.DEBUG) {
            //AuthPreferences(getApplication()).setSeedPhrase("ability salmon weapon apart awkward sudden glue budget fox paddle romance delay remove near clip cabbage exact off motion oxygen connect modify rotate vehicle")
            //AuthPreferences(getApplication()).setSeedPhrase("rifle vehicle onion typical base book trick child entry trick wedding festival zone sport coil verify mirror flame arena sustain coin state north blame")
            AuthPreferences(getApplication()).setSeedPhrase("chimney under robot witness female jaguar position rare acid puppy argue cancel kangaroo thing spring disease still predict movie announce throw entry lab tourist")
            _validateLiveData.value = true
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
                    AuthPreferences(getApplication()).setSeedPhrase(enteredPhrase)
                }
            } catch (ex: Exception) {
                Log.d(ex.message ?: "")
            }
        }
        _validateLiveData.value = success
    }
}
