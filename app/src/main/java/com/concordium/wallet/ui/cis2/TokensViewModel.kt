package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.AccountContractRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountContract
import com.concordium.wallet.data.room.ContractToken
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val waitingTokens: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val addingSelectedDone: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val stepPageBy: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tokenDetails: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val hasExistingAccountContract: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val nonSelected: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val contractTokens: MutableLiveData<List<ContractToken>> by lazy { MutableLiveData<List<ContractToken>>() }

    private val proxyRepository = ProxyRepository()

    fun loadTokens(accountAddress: String, isFungible: Boolean) {
        waiting.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            val accountContractRepository = AccountContractRepository(WalletDatabase.getDatabase(getApplication()).accountContractDao())
            val contractTokensRepository = ContractTokensRepository(WalletDatabase.getDatabase(getApplication()).contractTokenDao())
            val accountContracts = accountContractRepository.find(accountAddress)
            val contractTokens = mutableListOf<ContractToken>()
            accountContracts.forEach { accountContract ->
                contractTokens.addAll(contractTokensRepository.getTokensByContractIndex(accountContract.contractIndex))
            }
            waiting.postValue(false)
            this@TokensViewModel.contractTokens.postValue(contractTokens)
        }
    }

    fun lookForTokens(from: Int? = null) {
        if (!allowToLoadMore)
            return

        allowToLoadMore = false

        waitingTokens.postValue(true)

        CoroutineScope(Dispatchers.IO).launch {
            val contractTokensRepository = ContractTokensRepository(WalletDatabase.getDatabase(getApplication()).contractTokenDao())
            val existingContractTokens = contractTokensRepository.getTokensByContractIndex(tokenData.contractIndex)
            val existingTokens = existingContractTokens.map { it.tokenId }.toSet()
            proxyRepository.getCIS2Tokens(tokenData.contractIndex, "0", from, success = { cis2Tokens ->
                cis2Tokens.tokens.forEach { token ->
                    if (existingTokens.contains(token.id))
                        token.isSelected = true
                }
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
    }

    fun findTokenPositionById(tokenId: Int): Int {
        return tokens.indexOfFirst { it.id == tokenId }
    }

    fun toggleNewToken(token: Token) {
        tokens.firstOrNull { it.id == token.id }?.let {
            it.isSelected = it.isSelected == false
        }
    }

    fun hasExistingTokens() {
        val accountContractRepository = AccountContractRepository(WalletDatabase.getDatabase(getApplication()).accountContractDao())
        tokenData.account?.let { account ->
            viewModelScope.launch {
                val existingAccountContract = accountContractRepository.find(account.address, tokenData.contractIndex)
                hasExistingAccountContract.postValue(existingAccountContract != null)
            }
        } ?: run {
            hasExistingAccountContract.postValue(false)
        }
    }

    fun updateWithSelectedTokens() {
        val selectedTokens = tokens.filter { it.isSelected }
        tokenData.account?.let { account ->
            val accountContractRepository = AccountContractRepository(WalletDatabase.getDatabase(getApplication()).accountContractDao())
            val contractTokensRepository = ContractTokensRepository(WalletDatabase.getDatabase(getApplication()).contractTokenDao())
            CoroutineScope(Dispatchers.IO).launch {
                if (selectedTokens.isEmpty()) {
                    val existingContractTokens = contractTokensRepository.getTokensByContractIndex(tokenData.contractIndex)
                    existingContractTokens.forEach { existingContractToken ->
                        contractTokensRepository.delete(existingContractToken)
                    }
                    val existingAccountContract = accountContractRepository.find(account.address, tokenData.contractIndex)
                    if (existingAccountContract == null) {
                        nonSelected.postValue(true)
                    } else {
                        accountContractRepository.delete(existingAccountContract)
                        addingSelectedDone.postValue(true)
                    }
                } else {
                    val accountContract = accountContractRepository.find(account.address, tokenData.contractIndex)
                    if (accountContract == null) {
                        accountContractRepository.insert(AccountContract(0, account.address, tokenData.contractIndex))
                    }

                    val existingContractTokens = contractTokensRepository.getTokensByContractIndex(tokenData.contractIndex)
                    val existingNotSelectedTokenIds = existingContractTokens.map { it.tokenId }.minus(selectedTokens.map { it.id }.toSet())
                    existingNotSelectedTokenIds.forEach { existingNotSelectedTokenId ->
                        contractTokensRepository.find(tokenData.contractIndex, existingNotSelectedTokenId)?.let { existingNotSelectedContractToken ->
                            contractTokensRepository.delete(existingNotSelectedContractToken)
                        }
                    }

                    selectedTokens.forEach { selectedToken ->
                        val existingContractToken =  contractTokensRepository.find(tokenData.contractIndex, selectedToken.id)
                        if (existingContractToken == null)
                            contractTokensRepository.insert(ContractToken(0, tokenData.contractIndex, selectedToken.id))
                    }

                    addingSelectedDone.postValue(true)
                }
            }
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
