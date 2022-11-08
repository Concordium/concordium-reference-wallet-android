package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.walletconnect.WalletConnectData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Serializable

data class SendTokenData(
    var token: Token? = null,
    var account: Account? = null,
    var amount: Long = 0,
    var receiver: String = ""
): Serializable

class SendTokenViewModel(application: Application) : AndroidViewModel(application), Serializable {
    companion object {
        const val SEND_TOKEN_DATA = "SEND_TOKEN_DATA"
    }

    var sendTokenData = SendTokenData()
    val tokens: MutableLiveData<List<Token>> by lazy { MutableLiveData<List<Token>>() }
    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val transactionReady: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val feeReady: MutableLiveData<Long> by lazy { MutableLiveData<Long>() }

    fun loadTokens() {
        waiting.postValue(true)
        viewModelScope.launch {
            waiting.postValue(false)
            tokens.postValue(getMockTokens())
        }
    }

    fun send() {
        waiting.postValue(true)
        viewModelScope.launch {
            waiting.postValue(false)
            transactionReady.postValue(true)
        }
    }

    fun loadTransactionFee() {
        waiting.postValue(true)
        viewModelScope.launch {
            waiting.postValue(false)
            feeReady.postValue(12300)
        }
    }

    private fun getMockTokens() : List<Token> {
        val list = arrayListOf<Token>()
        list.add(Token(0, "asdgsdfgdsfg", "sdfgdsfgsdfg", ""))
        /*
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
        */
        return list
    }
}
