package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import kotlinx.coroutines.launch
import java.io.Serializable

data class TokenData(
    var account: Account? = null,
    var contractIndex: String = ""
): Serializable

class TokensViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TOKEN_DATA = "TOKEN_DATA"
    }

    private var allowToLoadMore = true

    var tokenData = TokenData()
    var tokens: MutableList<Token> = mutableListOf()

    //val tokens: MutableLiveData<List<Token>> by lazy { MutableLiveData<List<Token>>() }
    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val waitingTokens: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val addingSelectedDone: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val stepPageBy: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    private val proxyRepository = ProxyRepository()

    fun loadTokens(isFungible: Boolean) {
        waiting.postValue(true)
        viewModelScope.launch {
            //val accountRepository = AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
            //val tokensList = accountRepository.getAll().map { Token("", it.address, "DEF", 0) }.filter {
            //    if (isFungible) it.tokens name.startsWith("3q") else it.name.startsWith("3t")
            //}
            waiting.postValue(false)
            //tokens.postValue(tokensList)
        }
    }

    fun lookForTokens(from: Int? = null) {
        if (!allowToLoadMore)
            return

        allowToLoadMore = false

        from?.let {
            println("LC -> LoadFrom $it")
        }

        waitingTokens.postValue(true)
        proxyRepository.getCIS2Tokens(tokenData.contractIndex, "0", from, success = { cis2Tokens ->
            tokens.addAll(cis2Tokens.tokens)
            waitingTokens.postValue(false)
            allowToLoadMore = true
        }, failure = {
            waitingTokens.postValue(false)
            handleBackendError(it)
            allowToLoadMore = true
        })
    }

    fun toggleNewToken(token: Token) {
        //newTokens.firstOrNull { it.name == token.name }?.let {
        //    it.isSelected = it.isSelected == false
        //}
    }

    fun addSelectedTokens() {
        //val selectedTokens = newTokens.filter { it.isSelected == true }
        //println("LC -> selectedTokens = $selectedTokens")
        //viewModelScope.launch {
        //    delay(1000)
        //    addingSelectedDone.postValue(true)
        //}
    }

    fun stepPage(by: Int) {
        stepPageBy.postValue(by)
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }
/*
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
    }*/
}
