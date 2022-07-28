package com.concordium.wallet.ui.passphrase.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cash.z.ecc.android.bip39.Mnemonics
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.ui.passphrase.common.WordsPickedBaseListAdapter
import java.io.Serializable

class PassPhraseViewModel(application: Application) : AndroidViewModel(application), Serializable {
    var mnemonicCodeToConfirm = listOf<CharArray>()
    var wordsPicked = arrayOfNulls<String>(WORD_COUNT + (WordsPickedBaseListAdapter.OFFSET * 2) + 1)
    var passPhraseConfirmChecked: Boolean = false

    companion object {
        val WORD_COUNT: Int = Mnemonics.WordCount.COUNT_24.count
        const val PASS_PHRASE_DATA = "PASS_PHRASE_DATA"
    }

    private val _validateLiveData = MutableLiveData<Boolean>()
    val validate: LiveData<Boolean>
        get() = _validateLiveData

    val reveal = MutableLiveData<Boolean>()

    fun generateMnemonicCode() {
        val mnemonicCode: Mnemonics.MnemonicCode = Mnemonics.MnemonicCode(Mnemonics.WordCount.COUNT_24)
        mnemonicCode.forEach { word ->
            if (BuildConfig.DEBUG)
                println(word)
        }
        mnemonicCodeToConfirm = mnemonicCode.words.toList()
    }

    fun validateInputCode() {
        var success = false
        val entered = wordsPicked.filterNotNull().filter { it != WordsPickedBaseListAdapter.BLANK }
        if (entered.size < WORD_COUNT)
            return
        if (entered.size == WORD_COUNT) {
            val enteredPhrase: String = entered.joinToString(" ") { it }
            val generatedPhrase = mnemonicCodeToConfirm.joinToString(" ") { String(it) }
            success = enteredPhrase == generatedPhrase
        }
        _validateLiveData.value = success
    }
}
