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
import java.util.*

class PassPhraseRecoverViewModel(application: Application) : AndroidViewModel(application) {
    var wordsPicked = arrayOfNulls<String>(WORD_COUNT + (WordsPickedBaseListAdapter.OFFSET * 2) + 1)
    var allWords = listOf<String>()

    companion object {
        val WORD_COUNT: Int = Mnemonics.WordCount.COUNT_24.count
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
            //AuthPreferences(getApplication()).setSeedPhrase("rifle vehicle onion typical base book trick child entry trick wedding festival zone sport coil verify mirror flame arena sustain coin state north blame") // This should not work
            //AuthPreferences(getApplication()).setSeedPhrase("trust deal squeeze drastic sport squeeze evoke note fatigue peanut tissue crazy rough knock denial brick swift bus amateur just merit bind enforce peace") // Testnet ( created from iOS)
            AuthPreferences(getApplication()).setSeedPhrase("close orchard gesture rib income script attract wash surge badge catch page model option hire host tuition invite milk favorite foil kitchen glove brave")  // Testnet (created from Android)
            //AuthPreferences(getApplication()).setSeedPhrase("upset much whisper feature orient elder depth soap filter festival remove moon firm victory crew run dose wrist harsh height little grow split onion") // Testnet (created from Android)
            //AuthPreferences(getApplication()).setSeedPhrase("recall leader garden happy drift recycle raw swear cheap chimney sausage sugar bleak detail vapor fortune skill melody skin pledge physical gadget miss daring") // From Martin (left)
            //AuthPreferences(getApplication()).setSeedPhrase("ghost island ordinary student apart minimum enable diesel height twelve enemy exhaust obtain february code mix miss bounce true endless injury ankle scout tree") // From Martin (right)
            //AuthPreferences(getApplication()).setSeedPhrase("spider weird system jeans express patrol tooth answer solve gift defense carry sunset language ankle ability life popular swap donate toward obtain degree van") // From Martin (says not working)
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
