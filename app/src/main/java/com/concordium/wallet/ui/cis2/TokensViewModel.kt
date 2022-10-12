package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TokensViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var account: Account

    val tokens: MutableLiveData<List<Token>> by lazy { MutableLiveData<List<Token>>() }
    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val newTokens: MutableLiveData<List<Token>> by lazy { MutableLiveData<List<Token>>() }
    val waitingNewTokens: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val addingSelectedDone: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val stepPageBy: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    fun loadTokens(isFungible: Boolean) {
        waiting.postValue(true)
        viewModelScope.launch {
            val accountRepository = AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
            val tokensList = accountRepository.getAll().map { Token("", it.address, "DEF", 0) }.filter {
                if (isFungible) it.name.startsWith("3q") else it.name.startsWith("3t")
            }
            waiting.postValue(false)
            tokens.postValue(tokensList)
        }
    }

    fun lookForNewTokens(contractAddress: String) {
        waitingNewTokens.postValue(true)
        viewModelScope.launch {
            delay(1000)
            waitingNewTokens.postValue(false)
            newTokens.postValue(getMockTokens())
        }
    }

    fun toggleNewToken(token: Token) {
        newTokens.value?.firstOrNull { it.name == token.name }?.let {
            it.isSelected = it.isSelected == false
        }
    }

    fun addSelectedTokens() {
        newTokens.value?.let { tokens ->
            val selectedTokens = tokens.filter { it.isSelected == true }
            println("LC -> selectedTokens = $selectedTokens")
            viewModelScope.launch {
                delay(1000)
                addingSelectedDone.postValue(true)
            }
        }
    }

    fun cleanNewTokens() {
        newTokens.value = listOf()
    }

    fun stepPage(by: Int) {
        stepPageBy.postValue(by)
    }

    private fun getMockTokens() : List<Token> {
        val list = arrayListOf<Token>()
        list.add(Token("", "CCD", "CCD", 11000000000))
        list.add(Token("", "wCCD", "wCCD", 2000000000))
        list.add(Token("", "USDC", "USDC", 3100000000))
        list.add(Token("", "EC2", "EC2", 4004000000))
        list.add(Token("", "CCD", "CCD", 11000000000))
        list.add(Token("", "wCCD", "wCCD", 2000000000))
        list.add(Token("", "USDC", "USDC", 3100000000))
        list.add(Token("", "EC2", "EC2", 4004000000))
        list.add(Token("", "CCD", "CCD", 11000000000))
        list.add(Token("", "wCCD", "wCCD", 2000000000))
        list.add(Token("", "USDC", "USDC", 3100000000))
        list.add(Token("", "EC2", "EC2", 4004000000))
        list.add(Token("", "CCD", "CCD", 11000000000))
        list.add(Token("", "wCCD", "wCCD", 2000000000))
        list.add(Token("", "USDC", "USDC", 3100000000))
        list.add(Token("", "EC2", "EC2", 4004000000))
        list.add(Token("", "CCD", "CCD", 11000000000))
        list.add(Token("", "wCCD", "wCCD", 2000000000))
        list.add(Token("", "USDC", "USDC", 3100000000))
        list.add(Token("", "EC2", "EC2", 4004000000))
        list.add(Token("", "CCD", "CCD", 11000000000))
        list.add(Token("", "wCCD", "wCCD", 2000000000))
        list.add(Token("", "USDC", "USDC", 3100000000))
        list.add(Token("", "EC2", "EC2", 4004000000))
        list.add(Token("", "CCD", "CCD", 11000000000))
        list.add(Token("", "wCCD", "wCCD", 2000000000))
        list.add(Token("", "USDC", "USDC", 3100000000))
        list.add(Token("", "EC2", "EC2", 4004000000))
        return list
    }
}
