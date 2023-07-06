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
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountContract
import com.concordium.wallet.data.room.ContractToken
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.cis2.lookfornew.TokenSelectedDestination
import com.concordium.wallet.ui.cis2.retrofit.IncorrectChecksumException
import com.concordium.wallet.ui.cis2.retrofit.MetadataApiInstance
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.TokenUtil
import com.concordium.wallet.util.toBigInteger
import com.walletconnect.util.Empty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.Serializable
import java.math.BigInteger

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
        const val TOKENS_NOT_LOADED = -1
        const val TOKENS_OK = 0
        const val TOKENS_EMPTY = 1
        const val TOKENS_INVALID_INDEX = 2
        const val TOKENS_METADATA_ERROR = 3
        const val TOKENS_INVALID_CHECKSUM = 4
    }

    private var allowToLoadMore = true

    var tokenData = TokenData()
    var tokens: MutableList<Token> = mutableListOf()
    var searchedTokens: MutableList<Token> = mutableListOf()

    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val chooseTokenInfo: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    private val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val lookForTokens: MutableLiveData<Int> by lazy { MutableLiveData<Int>(TOKENS_NOT_LOADED) }
    val addTokenDestination: MutableLiveData<TokenSelectedDestination> by lazy {
        MutableLiveData<TokenSelectedDestination>(
            TokenSelectedDestination.NoChange
        )
    }
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
                        accountAddress,
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
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    it.contractName,
                    it.tokenMetadata?.symbol ?: ""
                )
            })
            waiting.postValue(false)
        }
    }

    fun lookForTokens(accountAddress: String, from: String? = null) {
        if (!allowToLoadMore)
            return

        allowToLoadMore = false

        if (from == null) {
            tokens.clear()
            lookForTokens.postValue(TOKENS_NOT_LOADED)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val contractTokensRepository = ContractTokensRepository(
                WalletDatabase.getDatabase(getApplication()).contractTokenDao()
            )
            val existingContractTokens =
                contractTokensRepository.getTokens(accountAddress, tokenData.contractIndex)
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
                    if (cis2Tokens.tokens.isEmpty()) {
                        lookForTokens.postValue(TOKENS_EMPTY)
                        allowToLoadMore = true
                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            val metadata = async { loadTokensMetadataUrls(cis2Tokens.tokens) }
                            loadTokensBalances()
                            val isSuccess = metadata.await()
                            if (isSuccess)
                                lookForTokens.postValue(TOKENS_OK)
                            allowToLoadMore = true
                        }
                    }
                },
                failure = {
                    lookForTokens.postValue(TOKENS_INVALID_INDEX)
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
        if (searchedTokens.isNotEmpty()) {
            updateSearchedTokens(searchedTokens)
        } else {
            updateTokens(tokens)
        }
    }

    private fun updateSearchedTokens(updatedTokens: MutableList<Token>) {
        tokenData.account?.let { account ->
            val accountContractRepository = AccountContractRepository(
                WalletDatabase.getDatabase(getApplication()).accountContractDao()
            )
            val contractTokensRepository = ContractTokensRepository(
                WalletDatabase.getDatabase(getApplication()).contractTokenDao()
            )
            CoroutineScope(Dispatchers.IO).launch {
                var anyChanges = false

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

                updatedTokens.forEach { selectedToken ->

                    val existingContractToken =
                        contractTokensRepository.find(
                            account.address,
                            tokenData.contractIndex,
                            selectedToken.token
                        )

                    if (existingContractToken == null && selectedToken.isSelected) {
                        contractTokensRepository.insert(
                            ContractToken(
                                0,
                                tokenData.contractIndex,
                                selectedToken.token,
                                account.address,
                                !(selectedToken.tokenMetadata?.unique ?: false),
                                selectedToken.tokenMetadata,
                                tokenData.contractName
                            )
                        )
                        anyChanges = true
                    }
                    if (existingContractToken != null && !selectedToken.isSelected) {
                        deleteSingleToken(
                            account.address, tokenData.contractIndex,
                            selectedToken.token
                        )
                        anyChanges = true
                    }
                }
                updateWithSelectedTokensDone.postValue(anyChanges)
            }
        }
    }

    private fun updateTokens(updatedTokens: MutableList<Token>) {
        val selectedTokens = updatedTokens.filter { it.isSelected }
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
                        contractTokensRepository.getTokens(account.address, tokenData.contractIndex)
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
                        contractTokensRepository.getTokens(account.address, tokenData.contractIndex)
                    val existingNotSelectedTokenIds = existingContractTokens.map { it.tokenId }
                        .minus(selectedTokens.map { it.id }.toSet())
                    existingNotSelectedTokenIds.forEach { existingNotSelectedTokenId ->
                        contractTokensRepository.find(
                            account.address,
                            tokenData.contractIndex,
                            existingNotSelectedTokenId
                        )?.let { existingNotSelectedContractToken ->
                            contractTokensRepository.delete(existingNotSelectedContractToken)
                            anyChanges = true
                        }
                    }
                    selectedTokens.forEach { selectedToken ->
                        val existingContractToken =
                            contractTokensRepository.find(
                                account.address,
                                tokenData.contractIndex,
                                selectedToken.id
                            )
                        if (existingContractToken == null) {
                            contractTokensRepository.insert(
                                ContractToken(
                                    0,
                                    tokenData.contractIndex,
                                    selectedToken.token,
                                    account.address,
                                    !(selectedToken.tokenMetadata?.unique ?: false),
                                    selectedToken.tokenMetadata,
                                    tokenData.contractName
                                )
                            )
                            anyChanges = true
                        }
                    }

                    val flowEndDestination = if (anyChanges) {
                        val numberOfNFTSelected = selectedTokens.filter {
                            it.tokenMetadata?.unique == true
                        }.size

                        when (numberOfNFTSelected) {
                            selectedTokens.size -> TokenSelectedDestination.NFT
                            0 -> TokenSelectedDestination.TOKEN
                            else -> TokenSelectedDestination.MIXED
                        }
                    } else {
                        TokenSelectedDestination.NoChange
                    }
                    addTokenDestination.postValue(flowEndDestination)
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
            account?.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance)
                ?: BigInteger.ZERO
        return TokenUtil.getCCDToken(account)
    }

    private suspend fun loadTokensMetadataUrls(tokens: List<Token>): Boolean {
        val commaSeparated = tokens.joinToString(",") { it.token }
        try {
            val cis2TokensMetadata =
                proxyRepository.getCIS2TokenMetadataSuspended(
                    tokenData.contractIndex,
                    tokenData.subIndex,
                    tokenIds = commaSeparated
                )
            tokenData.contractName = cis2TokensMetadata.contractName
            cis2TokensMetadata.metadata.forEach {
                loadTokenMetadata(tokenData.contractIndex, tokenData.contractName, it)
            }
            return true
        } catch (e: IncorrectChecksumException) {
            lookForTokens.postValue(TOKENS_INVALID_CHECKSUM)
            return false
        } catch (e: Throwable) {
            lookForTokens.postValue(TOKENS_METADATA_ERROR)
            return false
        }
    }

    private suspend fun loadTokenMetadata(
        contractIndex: String,
        contractName: String,
        cis2TokensMetadataItem: CIS2TokensMetadataItem
    ) {
        if (cis2TokensMetadataItem.metadataURL.isBlank())
            return
        val index =
            tokens.indexOfFirst { it.token == cis2TokensMetadataItem.tokenId && it.contractIndex == contractIndex }
        val metadata = MetadataApiInstance.safeMetadataCall(
            cis2TokensMetadataItem.metadataURL,
            cis2TokensMetadataItem.metadataChecksum
        ).getOrThrow()
        tokens[index].tokenMetadata = metadata
        tokens[index].contractIndex = contractIndex
        tokens[index].contractName = contractName
        tokenDetails.postValue(true)
    }

    fun loadTokensBalances() {
        if (tokenData.account == null)
            return

        tokens.filter { !it.isCCDToken }.groupBy { it.contractIndex }.forEach { group ->
            val groupTokens = group.value
            val commaSeparated = groupTokens.joinToString(",") { it.token }
            viewModelScope.launch {
                proxyRepository.getCIS2TokenBalance(group.key,
                    tokenData.subIndex,
                    tokenData.account!!.address,
                    commaSeparated,
                    success = { cis2TokensBalances ->
                        cis2TokensBalances.forEach { cis2TokenBalance ->
                            groupTokens.firstOrNull { it.token == cis2TokenBalance.tokenId }?.totalBalance =
                                cis2TokenBalance.balance.toBigInteger()
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

            val existingContractTokens =
                contractTokensRepository.getTokens(accountAddress, contractIndex)

            contractTokensRepository.find(accountAddress, contractIndex, tokenId)
                ?.let { existingNotSelectedContractToken ->
                    contractTokensRepository.delete(existingNotSelectedContractToken)

                    if (existingContractTokens.size == 1) {
                        val existingAccountContract =
                            accountContractRepository.find(accountAddress, contractIndex)
                        if (existingAccountContract == null) {
                            nonSelected.postValue(true)
                        } else {
                            accountContractRepository.delete(existingAccountContract)
                        }
                    }
                }
        }
    }

    fun onFindTokensDialogDismissed() {
        resetLookForTokens()
    }

    private fun resetLookForTokens() {
        tokenData.contractIndex = String.Empty
        stepPageBy.value = 0
        lookForTokens.value = TOKENS_NOT_LOADED
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }
}
