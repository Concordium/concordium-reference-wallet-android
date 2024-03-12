package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.AccountContractRepository
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.io.Serializable
import java.math.BigInteger

data class TokenData(
    var account: Account? = null,
    var selectedToken: Token? = null,
    var contractIndex: String = "",
    var subIndex: String = "0",
) : Serializable

class TokensViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TOKEN_DATA = "TOKEN_DATA"
        const val TOKENS_NOT_LOADED = -1
        const val TOKENS_OK = 0
        const val TOKENS_EMPTY = 1
    }

    private var allowToLoadMore = true

    var tokenData = TokenData()
    var tokens: MutableList<Token> = mutableListOf()
    var exactToken: Token? = null

    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val chooseTokenInfo: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val contactAddressLoading: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }
    private val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val lookForTokens: MutableLiveData<Int> by lazy { MutableLiveData<Int>(TOKENS_NOT_LOADED) }
    val lookForExactToken: MutableLiveData<Int> by lazy { MutableLiveData<Int>(TOKENS_NOT_LOADED) }
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
    private val contractTokensRepository: ContractTokensRepository by lazy {
        ContractTokensRepository(
            WalletDatabase.getDatabase(getApplication()).contractTokenDao()
        )
    }
    private val accountContractRepository: AccountContractRepository by lazy {
        AccountContractRepository(
            WalletDatabase.getDatabase(getApplication()).accountContractDao()
        )
    }

    fun loadTokens(accountAddress: String, isFungible: Boolean) {
        waiting.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
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

    fun lookForTokens(
        accountAddress: String,
        from: String? = null,
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (from != null && !allowToLoadMore)
            return@launch

        allowToLoadMore = false

        if (from == null) {
            tokens.clear()
            lookForTokens.postValue(TOKENS_NOT_LOADED)
        }

        val existingContractTokens =
            contractTokensRepository.getTokens(accountAddress, tokenData.contractIndex)
        val existingTokens = existingContractTokens.map { it.tokenId }.toSet()

        try {
            val pageLimit = 20
            val pageTokens = getFullyLoadedTokensPage(
                accountAddress = accountAddress,
                limit = pageLimit,
                from = from,
            ).onEach {
                it.isSelected = it.token in existingTokens
            }

            tokens.addAll(pageTokens)
            contactAddressLoading.postValue(false)
            allowToLoadMore = pageTokens.size >= pageLimit

            if (tokens.isEmpty() && !allowToLoadMore) {
                lookForTokens.postValue(TOKENS_EMPTY)
            } else {
                lookForTokens.postValue(TOKENS_OK)
            }
        } catch (e: Throwable) {
            handleBackendError(e)
            allowToLoadMore = true
            contactAddressLoading.postValue(false)
        }
    }

    /**
     * @return page of tokens with fully loaded metadata and balances.
     * If its size is smaller than [limit], consider this page the last one.
     */
    private suspend fun getFullyLoadedTokensPage(
        accountAddress: String,
        limit: Int,
        from: String? = null,
    ): List<Token> {
        val fullyLoadedTokens = mutableListOf<Token>()

        var tokenPageCursor = from
        var isLastTokenPage = false
        while (fullyLoadedTokens.size < limit && !isLastTokenPage) {
            val pageTokens = proxyRepository.getCIS2Tokens(
                index = tokenData.contractIndex,
                subIndex = tokenData.subIndex,
                limit = limit,
                from = tokenPageCursor,
            ).tokens.onEach {
                it.contractIndex = tokenData.contractIndex
                it.subIndex = tokenData.subIndex
            }

            isLastTokenPage = pageTokens.size < limit
            tokenPageCursor = pageTokens.lastOrNull()?.id

            loadTokensMetadata(pageTokens)
            val tokensWithMetadata = pageTokens.filter { it.tokenMetadata != null }
            loadTokensBalances(
                tokensToUpdate = tokensWithMetadata,
                accountAddress = accountAddress,
            )

            fullyLoadedTokens.addAll(tokensWithMetadata)
        }

        return fullyLoadedTokens
    }

    private suspend fun loadTokensMetadata(tokensToUpdate: List<Token>) {
        val tokensByContract: Map<String, List<Token>> = tokensToUpdate
            .filterNot(Token::isCCDToken)
            .groupBy(Token::contractIndex)

        tokensByContract.forEach { (contractIndex, contractTokens) ->
            val contractSubIndex = contractTokens.firstOrNull()?.subIndex
                ?: return@forEach

            contractTokens
                .chunked(ProxyRepository.CIS_2_TOKEN_METADATA_MAX_TOKEN_IDS)
                .forEach { contractTokensChunk ->
                    val commaSeparatedChunkTokenIds = contractTokensChunk.joinToString(
                        separator = ",",
                        transform = Token::token,
                    )

                    try {
                        val ciS2TokensMetadata = proxyRepository.getCIS2TokenMetadataV1(
                            index = contractIndex,
                            subIndex = contractSubIndex,
                            tokenIds = commaSeparatedChunkTokenIds,
                        )

                        ciS2TokensMetadata.metadata
                            .filterNot { it.metadataURL.isBlank() }
                            // Request the actual metadata in parallel.
                            .map { metadataItem ->
                                viewModelScope.async(Dispatchers.IO) {
                                    try {
                                        val verifiedMetadata = MetadataApiInstance.safeMetadataCall(
                                            url = metadataItem.metadataURL,
                                            checksum = metadataItem.metadataChecksum,
                                        ).getOrThrow()
                                        val correspondingToken = contractTokens.first {
                                            it.token == metadataItem.tokenId
                                        }
                                        correspondingToken.tokenMetadata = verifiedMetadata
                                        correspondingToken.contractName =
                                            ciS2TokensMetadata.contractName
                                    } catch (e: IncorrectChecksumException) {
                                        Log.w(
                                            "Metadata checksum incorrect:\n" +
                                                    "metadataItem=$metadataItem"
                                        )
                                    } catch (e: Throwable) {
                                        Log.e(
                                            "Failed to load metadata:\n" +
                                                    "metadataItem=$metadataItem" +
                                                    e
                                        )
                                    }
                                }
                            }
                            .awaitAll()
                    } catch (e: Throwable) {
                        Log.e(
                            "Failed to load metadata chunk:\n" +
                                    "contract=$contractIndex:$contractSubIndex,\n" +
                                    "chunkTokenIds=$commaSeparatedChunkTokenIds",
                            e
                        )
                    }
                }
        }
    }

    private suspend fun loadTokensBalances(
        tokensToUpdate: List<Token>,
        accountAddress: String,
    ) {
        val tokensByContract: Map<String, List<Token>> = tokensToUpdate
            .filterNot { it.totalSupply == "0" }
            .filterNot(Token::isCCDToken)
            .groupBy(Token::contractIndex)

        tokensByContract.forEach { (contractIndex, contractTokens) ->
            val contractSubIndex = contractTokens.firstOrNull()?.subIndex
                ?: return@forEach

            contractTokens
                .chunked(ProxyRepository.CIS_2_TOKEN_BALANCE_MAX_TOKEN_IDS)
                .forEach { contractTokensChunk ->
                    val commaSeparatedChunkTokenIds = contractTokensChunk.joinToString(
                        separator = ",",
                        transform = Token::token,
                    )

                    try {
                        proxyRepository.getCIS2TokenBalanceV1(
                            index = contractIndex,
                            subIndex = contractSubIndex,
                            accountAddress = accountAddress,
                            tokenIds = commaSeparatedChunkTokenIds,
                        ).forEach { balanceItem ->
                            val correspondingToken = contractTokens.first {
                                it.token == balanceItem.tokenId
                            }
                            correspondingToken.totalBalance = balanceItem.balance.toBigInteger()
                        }
                    } catch (e: Throwable) {
                        Log.e(
                            "Failed to load balances chunk:\n" +
                                    "contract=$contractIndex:$contractSubIndex,\n" +
                                    "accountAddress=$accountAddress,\n" +
                                    "chunkTokenIds=$commaSeparatedChunkTokenIds",
                            e
                        )
                    }
                }
        }
    }

    private var lookForExactTokenJob: Job? = null
    fun lookForExactToken(
        accountAddress: String,
        apparentTokenId: String,
    ) {
        lookForExactTokenJob?.cancel()
        lookForExactTokenJob = viewModelScope.launch(Dispatchers.IO) {
            val existingContractTokens =
                contractTokensRepository.getTokens(accountAddress, tokenData.contractIndex)
            val existingTokens = existingContractTokens.map { it.tokenId }.toSet()

            val apparentToken = Token(
                token = apparentTokenId,
                contractIndex = tokenData.contractIndex,
                subIndex = tokenData.subIndex,
                isSelected = apparentTokenId in existingTokens,
            )

            try {
                awaitAll(
                    async {
                        loadTokensMetadata(listOf(apparentToken))
                    },
                    async {
                        loadTokensBalances(
                            tokensToUpdate = listOf(apparentToken),
                            accountAddress = accountAddress,
                        )
                    },
                )

                if (apparentToken.tokenMetadata != null) {
                    exactToken = apparentToken
                    lookForExactToken.postValue(TOKENS_OK)
                } else {
                    exactToken = null
                    lookForExactToken.postValue(TOKENS_EMPTY)
                }
            } catch (e: Throwable) {
                handleBackendError(e)
                exactToken = null
                lookForExactToken.postValue(TOKENS_EMPTY)
            }
        }
    }

    fun dismissExactTokenLookup() {
        lookForExactTokenJob?.cancel()
        exactToken = null
        lookForExactToken.value = TOKENS_NOT_LOADED
    }

    fun toggleNewToken(token: Token) {
        // Update both unfiltered search result and the exact token
        // in case the target token is in both places.
        tokens.firstOrNull { it.token == token.token }?.let {
            it.isSelected = it.isSelected == false
        }
        exactToken?.takeIf { it.token == token.token }?.let {
            it.isSelected = it.isSelected == false
        }
    }

    fun hasExistingTokens() {
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
        val exactToken = this.exactToken
        if (exactToken != null) {
            updateSearchedTokens(listOf(exactToken))
        } else {
            updateTokens(tokens)
        }
    }

    private fun updateSearchedTokens(updatedTokens: Collection<Token>) {
        tokenData.account?.let { account ->
            viewModelScope.launch(Dispatchers.IO) {
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
                            selectedToken.contractIndex,
                            selectedToken.token
                        )

                    if (existingContractToken == null && selectedToken.isSelected) {
                        contractTokensRepository.insert(
                            ContractToken(
                                0,
                                selectedToken.contractIndex,
                                selectedToken.token,
                                account.address,
                                !(selectedToken.tokenMetadata?.unique ?: false),
                                selectedToken.tokenMetadata,
                                selectedToken.contractName
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

    private fun updateTokens(updatedTokens: Collection<Token>) {
        val selectedTokens = updatedTokens.filter { it.isSelected }
        tokenData.account?.let { account ->
            viewModelScope.launch(Dispatchers.IO) {
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
                    addTokenDestination.postValue(TokenSelectedDestination.MIXED)
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
                    val existingContractTokenIds = existingContractTokens.map { it.tokenId }
                    val unselectedTokenIds =
                        updatedTokens.filter { it.isSelected.not() }.map { it.token }
                    when {
                        existingContractTokenIds.any { unselectedTokenIds.contains(it) } ->
                            anyChanges = true

                        selectedTokens.map { it.token }
                            .containsAll(existingContractTokenIds).not() ->
                            anyChanges = true
                    }

                    val existingNotSelectedTokenIds =
                        existingContractTokens.map { it.tokenId }
                            .minus(selectedTokens.map { it.token }.toSet())
                    existingNotSelectedTokenIds.forEach { existingNotSelectedTokenId ->
                        contractTokensRepository.find(
                            account.address,
                            tokenData.contractIndex,
                            existingNotSelectedTokenId
                        )?.let { existingNotSelectedContractToken ->
                            contractTokensRepository.delete(existingNotSelectedContractToken)
                        }
                    }
                    selectedTokens.forEach { selectedToken ->
                        val existingContractToken =
                            contractTokensRepository.find(
                                account.address,
                                selectedToken.contractIndex,
                                selectedToken.token
                            )
                        if (existingContractToken == null) {
                            contractTokensRepository.insert(
                                ContractToken(
                                    0,
                                    selectedToken.contractIndex,
                                    selectedToken.token,
                                    account.address,
                                    !(selectedToken.tokenMetadata?.unique ?: false),
                                    selectedToken.tokenMetadata,
                                    selectedToken.contractName
                                )
                            )
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
        return TokenUtil.getCCDToken(account)
    }

    fun loadTokensBalances() {
        if (tokenData.account == null)
            return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                loadTokensBalances(
                    tokensToUpdate = tokens,
                    accountAddress = tokenData.account!!.address,
                )
                tokenBalances.postValue(true)
            } catch (e: Throwable) {
                handleBackendError(e)
            }
        }
    }

    fun deleteSingleToken(accountAddress: String, contractIndex: String, tokenId: String) =
        viewModelScope.launch(Dispatchers.IO) {
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

    fun onFindTokensDialogDismissed() {
        resetLookForTokens()
    }

    private fun resetLookForTokens() {
        tokenData.contractIndex = String.Empty
        stepPageBy.value = 0
        lookForTokens.value = TOKENS_NOT_LOADED
        lookForExactToken.value = TOKENS_NOT_LOADED
        exactToken = null
        allowToLoadMore = true
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }
}
