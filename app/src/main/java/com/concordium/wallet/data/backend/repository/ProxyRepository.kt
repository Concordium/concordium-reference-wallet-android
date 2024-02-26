package com.concordium.wallet.data.backend.repository

import com.concordium.wallet.App
import com.concordium.wallet.core.backend.BackendCallback
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.AccountKeyData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.AccountSubmissionStatus
import com.concordium.wallet.data.model.AccountTransactions
import com.concordium.wallet.data.model.AppSettings
import com.concordium.wallet.data.model.BakerPoolStatus
import com.concordium.wallet.data.model.CIS2Tokens
import com.concordium.wallet.data.model.CIS2TokensBalances
import com.concordium.wallet.data.model.CIS2TokensMetadata
import com.concordium.wallet.data.model.ChainParameters
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.SubmissionData
import com.concordium.wallet.data.model.TransferCost
import com.concordium.wallet.data.model.TransferSubmissionStatus
import java.math.BigInteger

class ProxyRepository {
    private val backend = App.appCore.getProxyBackend()

    companion object {
        const val SIMPLE_TRANSFER = "simpleTransfer"
        const val ENCRYPTED_TRANSFER = "encryptedTransfer"
        const val TRANSFER_TO_SECRET = "transferToSecret"
        const val TRANSFER_TO_PUBLIC = "transferToPublic"
        const val REGISTER_DELEGATION = "registerDelegation"
        const val UPDATE_DELEGATION = "updateDelegation"
        const val REMOVE_DELEGATION = "removeDelegation"
        const val REGISTER_BAKER = "registerBaker"
        const val UPDATE_BAKER_STAKE = "updateBakerStake"
        const val UPDATE_BAKER_POOL = "updateBakerPool"
        const val UPDATE_BAKER_KEYS = "updateBakerKeys"
        const val REMOVE_BAKER = "removeBaker"
        const val CONFIGURE_BAKER = "configureBaker"
        const val UPDATE = "update"
    }

    fun submitCredential(
        credentialWrapper: CredentialWrapper,
        success: (SubmissionData) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<SubmissionData> {
        val call = backend.submitCredential(credentialWrapper)
        call.enqueue(object : BackendCallback<SubmissionData>() {
            override fun onResponseData(response: SubmissionData) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getAccountSubmissionStatusSuspended(submissionId: String) =
        backend.accountSubmissionStatusSuspended(submissionId)

    fun getAppSettings(
        version: Int,
        success: (AppSettings) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<AppSettings> {
        val call = backend.appSettings("android", version)
        call.enqueue(object : BackendCallback<AppSettings>() {

            override fun onResponseData(response: AppSettings) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getBakerPool(
        bakerId: String,
        success: (BakerPoolStatus) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<BakerPoolStatus> {
        val call = backend.bakerPool(bakerId)
        call.enqueue(object : BackendCallback<BakerPoolStatus>() {
            override fun onResponseData(response: BakerPoolStatus) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getAccountSubmissionStatus(
        submissionId: String,
        success: (AccountSubmissionStatus) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<AccountSubmissionStatus> {
        val call = backend.accountSubmissionStatus(submissionId)
        call.enqueue(object : BackendCallback<AccountSubmissionStatus>() {
            override fun onResponseData(response: AccountSubmissionStatus) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getAccountNonce(
        accountAddress: String,
        success: (AccountNonce) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<AccountNonce> {
        val call = backend.accountNonce(accountAddress)
        call.enqueue(object : BackendCallback<AccountNonce>() {
            override fun onResponseData(response: AccountNonce) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun submitTransfer(
        transfer: CreateTransferOutput,
        success: (SubmissionData) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<SubmissionData> {
        val call = backend.submitTransfer(transfer)
        call.enqueue(object : BackendCallback<SubmissionData>() {
            override fun onResponseData(response: SubmissionData) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getTransferSubmissionStatusSuspended(submissionId: String) =
        backend.transferSubmissionStatusSuspended(submissionId)

    fun getTransferSubmissionStatus(
        submissionId: String,
        success: (TransferSubmissionStatus) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<TransferSubmissionStatus> {
        val call = backend.transferSubmissionStatus(submissionId)
        call.enqueue(object : BackendCallback<TransferSubmissionStatus>() {
            override fun onResponseData(response: TransferSubmissionStatus) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getTransferCost(
        type: String,
        memoSize: Int? = null,
        amount: BigInteger? = null,
        restake: Boolean? = null,
        lPool: Boolean? = null,
        targetChange: Boolean? = null,
        metadataSize: Int? = null,
        openStatus: String? = null,
        sender: String? = null,
        contractIndex: Int? = null,
        contractSubindex: Int? = null,
        receiveName: String? = null,
        parameter: String? = null,
        executionNRGBuffer: Int? = null,
        success: (TransferCost) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<TransferCost> {
        val lPoolArg = if (lPool == true) "lPool" else null
        val targetArg = if (targetChange == true) "target" else null
        val call = backend.transferCost(
            type,
            memoSize,
            amount?.toString(),
            restake,
            lPoolArg,
            targetArg,
            metadataSize,
            openStatus,
            sender,
            contractIndex,
            contractSubindex,
            receiveName,
            parameter,
            executionNRGBuffer
        )
        call.enqueue(object : BackendCallback<TransferCost>() {
            override fun onResponseData(response: TransferCost) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getChainParameters(
        success: (ChainParameters) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<ChainParameters> {
        val call = backend.chainParameters()
        call.enqueue(object : BackendCallback<ChainParameters>() {
            override fun onResponseData(response: ChainParameters) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getChainParametersSuspended() = backend.chainParametersSuspended()

    suspend fun getPassiveDelegationSuspended() = backend.passiveDelegationSuspended()

    suspend fun getBakerPoolSuspended(poolId: String) = backend.bakerPoolSuspended(poolId)

    suspend fun getAccountBalanceSuspended(accountAddress: String) =
        backend.accountBalanceSuspended(accountAddress)

    fun getAccountBalance(
        accountAddress: String,
        success: (AccountBalance) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<AccountBalance> {
        val call = backend.accountBalance(accountAddress)
        call.enqueue(object : BackendCallback<AccountBalance>() {
            override fun onResponseData(response: AccountBalance) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getAccountTransactions(
        accountAddress: String,
        success: (AccountTransactions) -> Unit,
        failure: ((Throwable) -> Unit)?,
        order: String? = "desc",
        from: Int? = null,
        limit: Int? = null,
        includeRewards: String? = "all"
    ): BackendRequest<AccountTransactions> {
        val call = backend.accountTransactions(accountAddress, order, from, limit, includeRewards)
        call.enqueue(object : BackendCallback<AccountTransactions>() {
            override fun onResponseData(response: AccountTransactions) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun requestGTUDrop(
        accountAddress: String,
        success: (SubmissionData) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<SubmissionData> {
        val call = backend.requestGTUDrop(accountAddress)
        call.enqueue(object : BackendCallback<SubmissionData>() {
            override fun onResponseData(response: SubmissionData) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getIGlobalInfo(
        success: (GlobalParamsWrapper) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<GlobalParamsWrapper> {
        val call = App.appCore.getProxyBackend().getGlobalInfo()
        call.enqueue(object : BackendCallback<GlobalParamsWrapper>() {
            override fun onResponseData(response: GlobalParamsWrapper) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getAccountEncryptedKey(
        accountAddress: String,
        success: (AccountKeyData) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<AccountKeyData> {
        val call = App.appCore.getProxyBackend().getAccountEncryptedKey(accountAddress)
        call.enqueue(object : BackendCallback<AccountKeyData>() {
            override fun onResponseData(response: AccountKeyData) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getCIS2Tokens(
        index: String,
        subIndex: String,
        from: String? = null,
        limit: Int? = null,
    ): CIS2Tokens = backend.cis2Tokens(index, subIndex, from, limit)

    suspend fun getCIS2TokenMetadataSuspended(
        index: String,
        subIndex: String,
        tokenIds: String,
    ): CIS2TokensMetadata = backend.cis2TokenMetadata(index, subIndex, tokenIds)

    suspend fun getCIS2TokenBalance(
        index: String,
        subIndex: String,
        accountAddress: String,
        tokenIds: String,
    ): CIS2TokensBalances = backend.cis2TokenBalance(index, subIndex, accountAddress, tokenIds)
}
