package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.AccountContractRepository
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val defaultToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }

    fun loadTokens() {
        waiting.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            val accountContractRepository = AccountContractRepository(WalletDatabase.getDatabase(getApplication()).accountContractDao())
            val contractTokensRepository = ContractTokensRepository(WalletDatabase.getDatabase(getApplication()).contractTokenDao())
            val tokensFound = mutableListOf<Token>()
            sendTokenData.account?.let { account ->
                val accountContracts = accountContractRepository.find(account.address)
                accountContracts.forEach { accountContract ->
                    val tokens = contractTokensRepository.getTokens(accountContract.contractIndex)
                    tokensFound.addAll(tokens.map { Token(it.tokenId, "", "", null, false, "") })
                }
            }
            waiting.postValue(false)
            tokens.postValue(tokensFound)
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

    fun loadCCDDefaultToken(accountAddress: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val accountRepository = AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
            val account = accountRepository.findByAddress(accountAddress)
            val atDisposal = account?.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance) ?: 0
            defaultToken.postValue(Token("", "CCD", "", null, false, "", true, account?.totalBalance ?: 0, atDisposal))
        }
    }
}
