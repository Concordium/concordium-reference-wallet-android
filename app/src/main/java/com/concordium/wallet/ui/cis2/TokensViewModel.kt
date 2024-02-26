package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.AccountContractRepository
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.CIS2Tokens
import com.concordium.wallet.data.model.CIS2TokensBalances
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Serializable
import java.math.BigInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    val contactAddressLoading: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }
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
    private val contractTokensRepository: ContractTokensRepository by lazy {
        ContractTokensRepository(
            WalletDatabase.getDatabase(getApplication()).contractTokenDao()
        )
    }

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
            val existingContractTokens =
                contractTokensRepository.getTokens(accountAddress, tokenData.contractIndex)
            val existingTokens = existingContractTokens.map { it.tokenId }.toSet()

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
        }
    }

    private suspend fun getFullyLoadedTokensPage(
        accountAddress: String,
        limit: Int,
        from: String? = null,
    ): List<Token> {
        val fullyLoadedTokens = mutableListOf<Token>()

        var tokenPageCursor = from
        var isLastTokenPage = false
        // Load raw tokens in smaller batches to avoid unnecessary loading of metadata.
        val tokenPageLimit = (limit / 2).coerceAtLeast(5)
        while (fullyLoadedTokens.size < limit && !isLastTokenPage) {
            val pageTokens = getTokensPage(
                contractIndex = tokenData.contractIndex,
                contractSubIndex = tokenData.subIndex,
                limit = tokenPageLimit,
                from = tokenPageCursor,
            ).tokens.onEach {
                it.contractIndex = tokenData.contractIndex
                it.subIndex = tokenData.subIndex
            }

            isLastTokenPage = pageTokens.size < tokenPageLimit
            tokenPageCursor = pageTokens.lastOrNull()?.id

            loadTokensMetadata(pageTokens)
            val tokensWithMetadata = pageTokens.filter { it.tokenMetadata != null }
            loadTokensBalances(
                tokens = tokensWithMetadata,
                accountAddress = accountAddress,
            )

            fullyLoadedTokens.addAll(tokensWithMetadata)
        }

        return fullyLoadedTokens
    }

    // TODO make 'getCIS2Tokens' call suspend within the repository.
    private suspend fun getTokensPage(
        contractIndex: String,
        contractSubIndex: String,
        limit: Int,
        from: String?,
    ) = suspendCancellableCoroutine { continuation ->
        val backendRequest: BackendRequest<CIS2Tokens> = proxyRepository.getCIS2Tokens(
            index = contractIndex,
            subIndex = contractSubIndex,
            limit = limit,
            from = from,
            success = continuation::resume,
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    private suspend fun loadTokensMetadata(tokens: List<Token>) {
        // Load the metadata items one by one instead of batch
        // as the batch request fails in case one of the tokens have invalid data.
        tokens.forEach { token ->
            try {
                proxyRepository.getCIS2TokenMetadataSuspended(
                    index = token.contractIndex,
                    subIndex = token.subIndex,
                    tokenIds = token.token,
                )
                    .metadata
                    .filterNot { it.metadataURL.isBlank() }
                    .forEach { metadataItem ->
                        val verifiedMetadata = MetadataApiInstance.safeMetadataCall(
                            url = metadataItem.metadataURL,
                            checksum = metadataItem.metadataChecksum,
                        ).getOrThrow()
                        token.tokenMetadata = verifiedMetadata
                    }
            } catch (e: IncorrectChecksumException) {
                Log.w(
                    "Metadata checksum incorrect:\n" +
                            "token=$token"
                )
            } catch (e: Throwable) {
                Log.e(
                    "Failed to load metadata:\n" +
                            "token=$token", e
                )
            }
        }
    }

    private suspend fun loadTokensBalances(
        tokens: List<Token>,
        accountAddress: String,
    ) {
        val tokensByContract: Map<String, List<Token>> = tokens
            .filterNot { it.totalSupply == "0" }
            .filterNot(Token::isCCDToken)
            .groupBy(Token::contractIndex)

        tokensByContract.forEach { (contractIndex, contractTokens) ->
            val commaSeparatedTokenIds = contractTokens.joinToString(
                separator = ",",
                transform = Token::token,
            )
            val contractSubIndex = contractTokens.firstOrNull()?.subIndex
                ?: return@forEach

            try {
                getTokenBalances(
                    contractIndex = contractIndex,
                    contractSubIndex = contractSubIndex,
                    accountAddress = accountAddress,
                    commaSeparatedTokenIds = commaSeparatedTokenIds,
                ).forEach { balanceItem ->
                    val correspondingToken = contractTokens.first {
                        it.token == balanceItem.tokenId
                    }
                    correspondingToken.totalBalance = balanceItem.balance.toBigInteger()
                }
            } catch (e: Throwable) {
                Log.e(
                    "Failed to load balances:\n" +
                            "contract=$contractIndex:$contractSubIndex,\n" +
                            "accountAddress=$accountAddress",
                    e
                )
            }
        }
    }

    // TODO make 'getCIS2TokenBalance' call suspend within the repository.
    private suspend fun getTokenBalances(
        contractIndex: String,
        contractSubIndex: String,
        accountAddress: String,
        commaSeparatedTokenIds: String,
    ) = suspendCancellableCoroutine { continuation ->
        val backendRequest: BackendRequest<CIS2TokensBalances> =
            proxyRepository.getCIS2TokenBalance(
                index = contractIndex,
                subIndex = contractSubIndex,
                tokenIds = commaSeparatedTokenIds,
                accountAddress = accountAddress,
                success = continuation::resume,
                failure = continuation::resumeWithException,
            )
        continuation.invokeOnCancellation { backendRequest.dispose() }
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
                                tokenData.contractIndex,
                                selectedToken.token
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

    private fun loadTokensMetadata() = viewModelScope.launch(Dispatchers.IO) {
        val commaSeparated = tokens.filter { !it.isCCDToken }.joinToString(",") { it.token }
        try {
            val cis2TokensMetadata =
                proxyRepository.getCIS2TokenMetadataSuspended(
                    tokenData.contractIndex,
                    tokenData.subIndex,
                    tokenIds = commaSeparated
                )
            tokenData.contractName = cis2TokensMetadata.contractName
            cis2TokensMetadata.metadata.forEach { metadataItem ->
                loadTokenMetadata(
                    tokenToUpdate = tokens.first {
                        it.token == metadataItem.tokenId
                                && it.contractIndex == tokenData.contractIndex
                    },
                    cis2TokensMetadataItem = metadataItem,
                )
            }
        } catch (e: IncorrectChecksumException) {
            lookForTokens.postValue(TOKENS_INVALID_CHECKSUM)
        } catch (e: Throwable) {
            lookForTokens.postValue(TOKENS_METADATA_ERROR)
        }
    }

    private suspend fun loadTokenMetadata(
        tokenToUpdate: Token,
        cis2TokensMetadataItem: CIS2TokensMetadataItem
    ) {
        if (cis2TokensMetadataItem.metadataURL.isBlank())
            return
        val metadata = MetadataApiInstance.safeMetadataCall(
            cis2TokensMetadataItem.metadataURL,
            cis2TokensMetadataItem.metadataChecksum
        ).getOrThrow()
        tokenToUpdate.tokenMetadata = metadata
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
        allowToLoadMore = true
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }
}
