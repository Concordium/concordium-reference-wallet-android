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
    var newTokens: List<Token> = listOf() //MutableLiveData<List<Token>> by lazy { MutableLiveData<List<Token>>() }
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
        newTokens = listOf()
        waitingNewTokens.postValue(true)
        viewModelScope.launch {
            delay(1000)
            newTokens = getMockTokens()
            waitingNewTokens.postValue(false)
        }
    }

    fun toggleNewToken(token: Token) {
        newTokens.firstOrNull { it.name == token.name }?.let {
            it.isSelected = it.isSelected == false
        }
    }

    fun addSelectedTokens() {
        val selectedTokens = newTokens.filter { it.isSelected == true }
        println("LC -> selectedTokens = $selectedTokens")
        viewModelScope.launch {
            delay(1000)
            addingSelectedDone.postValue(true)
        }
    }

    fun stepPage(by: Int) {
        stepPageBy.postValue(by)
    }

    private fun getMockTokens() : List<Token> {
        val list = arrayListOf<Token>()
        list.add(Token("", "01 CCD", "CCD", 11000000000))
        list.add(Token("", "02 wCCD", "wCCD", 2000000000))
        list.add(Token("", "03 USDC", "USDC", 3100000000))
        list.add(Token("", "04 EC2", "EC2", 4004000000))
        list.add(Token("", "05 CCD", "CCD", 11000000000))
        list.add(Token("", "06 wCCD", "wCCD", 2000000000))
        list.add(Token("", "07 USDC", "USDC", 3100000000))
        list.add(Token("", "08 EC2", "EC2", 4004000000))
        list.add(Token("", "09 CCD", "CCD", 11000000000))
        list.add(Token("", "10 wCCD", "wCCD", 2000000000))
        list.add(Token("", "11 USDC", "USDC", 3100000000))
        list.add(Token("", "12 EC2", "EC2", 4004000000))
        list.add(Token("", "13 CCD", "CCD", 11000000000))
        list.add(Token("", "14 wCCD", "wCCD", 2000000000))
        list.add(Token("", "15 USDC", "USDC", 3100000000))
        list.add(Token("", "16 EC2", "EC2", 4004000000))
        list.add(Token("", "17 CCD", "CCD", 11000000000))
        list.add(Token("", "18 wCCD", "wCCD", 2000000000))
        list.add(Token("", "19 USDC", "USDC", 3100000000))
        list.add(Token("", "20 EC2", "EC2", 4004000000))
        list.add(Token("", "21 CCD", "CCD", 11000000000))
        list.add(Token("", "22 wCCD", "wCCD", 2000000000))
        list.add(Token("", "23 USDC", "USDC", 3100000000))
        list.add(Token("", "24 EC2", "EC2", 4004000000))
        list.add(Token("", "25 CCD", "CCD", 11000000000))
        list.add(Token("", "26 wCCD", "wCCD", 2000000000))
        list.add(Token("", "27 USDC", "USDC", 3100000000))
        list.add(Token("", "28 EC2", "EC2", 4004000000))
        return list
    }
}
