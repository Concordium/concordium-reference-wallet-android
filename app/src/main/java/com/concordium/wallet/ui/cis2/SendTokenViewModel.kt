package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SendTokenViewModel(application: Application) : AndroidViewModel(application) {
    var token: Token? = null
    lateinit var account: Account

    val tokens: MutableLiveData<List<Token>> by lazy { MutableLiveData<List<Token>>() }
    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val transactionReady: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    fun loadTokens() {
        waiting.postValue(true)
        viewModelScope.launch {
            delay(2000)
            waiting.postValue(false)
            tokens.postValue(getMockTokens())
        }
    }

    fun send() {
        waiting.postValue(true)
        viewModelScope.launch {
            delay(2000)
            waiting.postValue(false)
            transactionReady.postValue(true)
        }
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
