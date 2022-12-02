package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.AccountContractRepository
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.CIS2TokensMetadataItem
import com.concordium.wallet.data.model.Thumbnail
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountContract
import com.concordium.wallet.data.room.ContractToken
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.cis2.retrofit.MetadataApiInstance
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Serializable

data class TokenData(
    var account: Account? = null,
    var selectedToken: Token? = null,
    var contractIndex: String = "",
    var subIndex: String = "0",
    var contractName: String = ""
) : Serializable

class TokensViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TOKEN_DATA = "TOKEN_DATA"
    }

    private var allowToLoadMore = true

    var tokenData = TokenData()
    var tokens: MutableList<Token> = mutableListOf()

    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val chooseTokenInfo: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val lookForTokens: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val waitingTokens: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val updateWithSelectedTokensDone: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val stepPageBy: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tokenDetails: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val hasExistingAccountContract: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val nonSelected: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val tokenBalances: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    private val proxyRepository = ProxyRepository()

    fun loadTokens(accountAddress: String, isFungible: Boolean) {
        waiting.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            val accountContractRepository = AccountContractRepository(
                WalletDatabase.getDatabase(getApplication()).accountContractDao()
            )
            val contractTokensRepository = ContractTokensRepository(
                WalletDatabase.getDatabase(getApplication()).contractTokenDao()
            )
            val accountContracts = accountContractRepository.find(accountAddress)
            val contractTokens = mutableListOf<ContractToken>()
            accountContracts.forEach { accountContract ->
                contractTokens.addAll(
                    contractTokensRepository.getTokens(
                        accountContract.contractIndex,
                        isFungible
                    )
                )

            }
            tokens.clear()
            if (isFungible) {
                // On fungible tab we add CCD as default at the top
                tokens.add(getCCDDefaultToken(accountAddress))
            }
            tokens.addAll(contractTokens.map {
                Token(
                    it.tokenId,
                    it.tokenId,
                    "",
                    it.tokenMetadata,
                    true,
                    it.contractIndex,
                    tokenData.subIndex,
                    false,
                    0,
                    0,
                    it.contractName,
                    it.tokenMetadata?.symbol ?: ""
                )
            })
            waiting.postValue(false)
        }
    }

    fun lookForTokens(from: String? = null) {
        if (!allowToLoadMore)
            return

        allowToLoadMore = false

        lookForTokens.postValue(true)

        CoroutineScope(Dispatchers.IO).launch {
            val contractTokensRepository = ContractTokensRepository(
                WalletDatabase.getDatabase(getApplication()).contractTokenDao()
            )
            val existingContractTokens = contractTokensRepository.getTokens(tokenData.contractIndex)
            val existingTokens = existingContractTokens.map { it.tokenId }.toSet()
            proxyRepository.getCIS2Tokens(
                tokenData.contractIndex,
                tokenData.subIndex,
                from,
                success = { cis2Tokens ->
                    cis2Tokens.tokens.forEach { token ->
                        if (existingTokens.contains(token.token)) {
                            token.isSelected = true
                        }

                        token.contractIndex = tokenData.contractIndex
                        token.subIndex = tokenData.subIndex
                    }
                    tokens.addAll(cis2Tokens.tokens)
                    loadTokensMetadataUrls(cis2Tokens.tokens)
                    lookForTokens.postValue(false)
                    allowToLoadMore = true
                },
                failure = {
                    lookForTokens.postValue(false)
                    handleBackendError(it)
                    allowToLoadMore = true
                })
        }
    }

    fun toggleNewToken(token: Token) {
        tokens.firstOrNull { it.id == token.id }?.let {
            it.isSelected = it.isSelected == false
        }
    }

    fun hasExistingTokens() {
        val accountContractRepository = AccountContractRepository(
            WalletDatabase.getDatabase(getApplication()).accountContractDao()
        )
        tokenData.account?.let { account ->
            viewModelScope.launch {
                val existingAccountContract =
                    accountContractRepository.find(account.address, tokenData.contractIndex)
                hasExistingAccountContract.postValue(existingAccountContract != null)
            }
        } ?: run {
            hasExistingAccountContract.postValue(false)
        }
    }

    fun updateWithSelectedTokens() {
        val selectedTokens = tokens.filter { it.isSelected }
        tokenData.account?.let { account ->
            val accountContractRepository = AccountContractRepository(
                WalletDatabase.getDatabase(getApplication()).accountContractDao()
            )
            val contractTokensRepository = ContractTokensRepository(
                WalletDatabase.getDatabase(getApplication()).contractTokenDao()
            )
            CoroutineScope(Dispatchers.IO).launch {
                var anyChanges = false
                if (selectedTokens.isEmpty()) {
                    val existingContractTokens =
                        contractTokensRepository.getTokens(tokenData.contractIndex)
                    existingContractTokens.forEach { existingContractToken ->
                        contractTokensRepository.delete(existingContractToken)
                        anyChanges = true
                    }
                    val existingAccountContract =
                        accountContractRepository.find(account.address, tokenData.contractIndex)
                    if (existingAccountContract == null) {
                        nonSelected.postValue(true)
                    } else {
                        accountContractRepository.delete(existingAccountContract)
                        anyChanges = true
                    }
                    updateWithSelectedTokensDone.postValue(anyChanges)
                } else {
                    val accountContract =
                        accountContractRepository.find(account.address, tokenData.contractIndex)
                    if (accountContract == null) {
                        accountContractRepository.insert(
                            AccountContract(
                                0,
                                account.address,
                                tokenData.contractIndex
                            )
                        )
                        anyChanges = true
                    }
                    val existingContractTokens =
                        contractTokensRepository.getTokens(tokenData.contractIndex)
                    val existingNotSelectedTokenIds = existingContractTokens.map { it.tokenId }
                        .minus(selectedTokens.map { it.id }.toSet())
                    existingNotSelectedTokenIds.forEach { existingNotSelectedTokenId ->
                        contractTokensRepository.find(
                            tokenData.contractIndex,
                            existingNotSelectedTokenId
                        )?.let { existingNotSelectedContractToken ->
                            contractTokensRepository.delete(existingNotSelectedContractToken)
                            anyChanges = true
                        }
                    }
                    selectedTokens.forEach { selectedToken ->
                        val existingContractToken =
                            contractTokensRepository.find(tokenData.contractIndex, selectedToken.id)
                        if (existingContractToken == null) {
                            contractTokensRepository.insert(
                                ContractToken(
                                    0,
                                    tokenData.contractIndex,
                                    selectedToken.token,
                                    selectedToken.tokenMetadata?.unique ?: false,
                                    selectedToken.tokenMetadata,
                                    tokenData.contractName
                                )
                            )
                            anyChanges = true
                        }
                    }
                    updateWithSelectedTokensDone.postValue(anyChanges)
                }
            }
        }
    }

    fun stepPage(by: Int) {
        stepPageBy.postValue(by)
    }

    private suspend fun getCCDDefaultToken(accountAddress: String): Token {
        val accountRepository =
            AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
        val account = accountRepository.findByAddress(accountAddress)
        val atDisposal =
            account?.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance) ?: 0
        return Token(
            "",
            "CCD",
            "",
            null,
            false,
            "",
            "",
            true,
            account?.totalBalance ?: 0,
            atDisposal,
            "",
            "CCD"
        )
    }

    private fun loadTokensMetadataUrls(tokens: List<Token>) {
        viewModelScope.launch {
            val commaSeparated = tokens.joinToString(",") { it.token }
            proxyRepository.getCIS2TokenMetadata(
                tokenData.contractIndex,
                tokenData.subIndex,
                tokenIds = commaSeparated,
                success = { cis2TokensMetadata ->
                    tokenData.contractName = cis2TokensMetadata.contractName
                    cis2TokensMetadata.metadata.forEach {
                        loadTokenMetadata(tokenData.contractIndex, tokenData.contractName, it)
                    }
                },
                failure = {
                    handleBackendError(it)
                })
        }
    }

    private fun loadTokenMetadata(
        contractIndex: String,
        contractName: String,
        cis2TokensMetadataItem: CIS2TokensMetadataItem
    ) {
        println("LC -> ${cis2TokensMetadataItem.metadataURL}")
        if (cis2TokensMetadataItem.metadataURL.isBlank())
            return
        viewModelScope.launch {
            val index =
                tokens.indexOfFirst { it.token == cis2TokensMetadataItem.tokenId && it.contractIndex == contractIndex }
            if (tokens.count() > index && index >= 0) {
                val tokenMetadata =
                    MetadataApiInstance.safeMetadataCall(cis2TokensMetadataItem.metadataURL)

                if (tokenMetadata != null) {
                    tokens[index].tokenMetadata = tokenMetadata
                } else {
                    tokens[index].tokenMetadata = TokenMetadata(
                        -1,
                        "",
                        "",
                        "",
                        Thumbnail("none"),
                        false,
                        null,
                        null,
                        null,
                        null,
                        null
                    )
                }
                tokens[index].contractIndex = contractIndex
                tokens[index].contractName = contractName
                tokenDetails.postValue(true)
            }
        }
    }

    fun loadTokensBalances() {
        if (tokenData.account == null)
            return

        tokens.filter { !it.isCCDToken }.groupBy { it.contractIndex }.forEach { group ->
            val commaSeparated = group.value.joinToString(",") { it.token }
            viewModelScope.launch {
                proxyRepository.getCIS2TokenBalance(group.key,
                    tokenData.subIndex,
                    tokenData.account!!.address,
                    commaSeparated,
                    success = { cis2TokensBalances ->
                        cis2TokensBalances.forEach { cis2TokenBalance ->
                            tokens.firstOrNull { it.token == cis2TokenBalance.tokenId }?.totalBalance =
                                cis2TokenBalance.balance.toLong()
                        }
                        tokenBalances.postValue(true)
                    },
                    failure = {
                        handleBackendError(it)
                    }
                )
            }
        }
    }

    fun deleteSingleToken(accountAddress: String, contractIndex: String, tokenId: String) {
        val contractTokensRepository = ContractTokensRepository(
            WalletDatabase.getDatabase(getApplication()).contractTokenDao()
        )
        val accountContractRepository = AccountContractRepository(
            WalletDatabase.getDatabase(getApplication()).accountContractDao()
        )

        CoroutineScope(Dispatchers.IO).launch {
            Log.d("ATTEMPTING DELETE SCOPE LOUNCHED : Contract Index: $contractIndex, TokenId: $tokenId ")

            val existingContractTokens =
                contractTokensRepository.getTokens(contractIndex)

            val token = contractTokensRepository.find(contractIndex, tokenId)
            if (token == null) {
                Log.d("TOKEN IS NULL")
            }
            contractTokensRepository.find(contractIndex, tokenId)
                ?.let { existingNotSelectedContractToken ->
                    Log.d("ATTEMPTING DELETE : ${existingNotSelectedContractToken.tokenId}")
                    contractTokensRepository.delete(existingNotSelectedContractToken)
                    Log.d("DELETED : ${existingNotSelectedContractToken.tokenId}")

                    if (existingContractTokens.size == 1) {
                        val existingAccountContract =
                            accountContractRepository.find(accountAddress, tokenData.contractIndex)
                        if (existingAccountContract == null) {
                            nonSelected.postValue(true)
                        } else {
                            accountContractRepository.delete(existingAccountContract)
                        }
                    }
                }
        }
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }
}
