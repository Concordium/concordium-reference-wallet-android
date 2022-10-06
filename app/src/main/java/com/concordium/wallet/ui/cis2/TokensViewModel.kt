package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.launch

class TokensViewModel(application: Application) : AndroidViewModel(application) {
    val tokens: MutableLiveData<List<Token>> by lazy { MutableLiveData<List<Token>>() }
    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    fun loadTokens(isFungible: Boolean) {
        waiting.postValue(true)
        viewModelScope.launch {
            val accountRepository = AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
            val tokensList = accountRepository.getAll().map { Token("", it.address) }.filter {
                if (isFungible) it.name.startsWith("3q") else it.name.startsWith("3t")
            }
            waiting.postValue(false)
            tokens.postValue(tokensList)
        }
    }
}
