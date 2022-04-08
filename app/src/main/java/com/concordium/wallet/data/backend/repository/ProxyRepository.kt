package com.concordium.wallet.data.backend.repository

import com.concordium.wallet.App
import com.concordium.wallet.core.backend.BackendCallback
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.model.*

class ProxyRepository() {

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
        const val UPDATE_BAKE_STAKE = "updateBakerStake"
        const val UPDATE_BAKER_POOL = "updateBakerPool"
        const val UPDATE_BAKER_KEYS = "updateBakerKeys"
        const val REMOVE_BAKER = "removeBaker"
        const val CONFIGURE_BAKER = "configureBaker"
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

        return BackendRequest<SubmissionData>(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getAccountSubmissionStatusSuspended(submissionId: String) = backend.accountSubmissionStatusSuspended(submissionId)


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

        return BackendRequest<BakerPoolStatus>(
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

        return BackendRequest<AccountSubmissionStatus>(
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

        return BackendRequest<AccountNonce>(
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

        return BackendRequest<SubmissionData>(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getTransferSubmissionStatusSuspended(submissionId: String) = backend.transferSubmissionStatusSuspended(submissionId)

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

        return BackendRequest<TransferSubmissionStatus>(
            call = call,
            success = success,
            failure = failure
        )
    }


    fun getTransferCost(type: String, memoSize: Int?, amount: Long? = null, restake: Boolean? = null, lPool: Boolean? = null, success: (TransferCost) -> Unit, failure: ((Throwable) -> Unit)?): BackendRequest<TransferCost> {
        val lPoolArg = if(lPool == true) "lPool" else null
        val call = backend.transferCost(type, memoSize, amount, restake, lPoolArg)

        call.enqueue(object : BackendCallback<TransferCost>() {

            override fun onResponseData(response: TransferCost) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })

        return BackendRequest<TransferCost>(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getChainParameters(success: (ChainParameters) -> Unit, failure: ((Throwable) -> Unit)?): BackendRequest<ChainParameters> {
        val call = backend.chainParameters()

        call.enqueue(object : BackendCallback<ChainParameters>() {

            override fun onResponseData(response: ChainParameters) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })

        return BackendRequest<ChainParameters>(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getAccountBalanceSuspended(accountAddress: String) = backend.accountBalanceSuspended(accountAddress)

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

        return BackendRequest<AccountBalance>(
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

        return BackendRequest<AccountTransactions>(
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

        return BackendRequest<SubmissionData>(
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

        return BackendRequest<GlobalParamsWrapper>(
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

        return BackendRequest<AccountKeyData>(
            call = call,
            success = success,
            failure = failure
        )
    }
}