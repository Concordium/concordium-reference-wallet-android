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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Serializable
import kotlin.random.Random

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
    val tokenDetails: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

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

        waitingTokens.postValue(true)
        proxyRepository.getCIS2Tokens(tokenData.contractIndex, "0", from, success = { cis2Tokens ->
            tokens.addAll(cis2Tokens.tokens)
            loadTokenDetails(cis2Tokens.tokens)
            waitingTokens.postValue(false)
            allowToLoadMore = true
        }, failure = {
            waitingTokens.postValue(false)
            handleBackendError(it)
            allowToLoadMore = true
        })
    }

    fun findTokenPositionById(tokenId: Int): Int {
        return tokens.indexOfFirst { it.id == tokenId }
    }

    fun toggleNewToken(token: Token) {
        tokens.firstOrNull { it.id == token.id }?.let {
            it.isSelected = it.isSelected == false
        }
    }

    fun addSelectedTokens() {
        val selectedTokens = tokens.filter { it.isSelected }
        println("LC -> selectedTokens = ${selectedTokens.map { it.id }}")
        viewModelScope.launch {
            delay(1000)
            addingSelectedDone.postValue(true)
        }
    }

    fun stepPage(by: Int) {
        stepPageBy.postValue(by)
    }

    private fun loadTokenDetails(tokens: List<Token>) {
        viewModelScope.launch {
            tokens.forEach { token ->
                delay(Random.nextLong(200, 500))
                token.token = "UPDATED" + token.token
                token.imageUrl = "https://picsum.photos/200"
                tokenDetails.postValue(token.id)
            }
        }
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }
}
