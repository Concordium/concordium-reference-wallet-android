package com.concordium.wallet.data.backend

import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.model.*
import retrofit2.Call
import retrofit2.http.*

interface ProxyBackend {

    @PUT("v0/submitCredential")
    fun submitCredential(@Body credential: CredentialWrapper): Call<SubmissionData>

    @GET("v0/bakerPool/{poolId}")
    fun bakerPool(@Path("poolId") poolId: String): Call<BakerPoolStatus>

    @GET("v0/submissionStatus/{submissionId}")
    fun accountSubmissionStatus(@Path("submissionId") submissionId: String): Call<AccountSubmissionStatus>

    @GET("v0/submissionStatus/{submissionId}")
    suspend fun accountSubmissionStatusSuspended(@Path("submissionId") submissionId: String): AccountSubmissionStatus

    @GET("v0/accNonce/{accountAddress}")
    fun accountNonce(@Path("accountAddress") accountAddress: String): Call<AccountNonce>

    @PUT("v0/submitTransfer")
    fun submitTransfer(@Body transfer: CreateTransferOutput): Call<SubmissionData>

    @GET("v0/submissionStatus/{submissionId}")
    fun transferSubmissionStatus(@Path("submissionId") submissionId: String): Call<TransferSubmissionStatus>

    @GET("v0/submissionStatus/{submissionId}")
    suspend fun transferSubmissionStatusSuspended(@Path("submissionId") submissionId: String): TransferSubmissionStatus

    @GET("v0/transactionCost")
    fun transferCost(
        @Query("type") type: String,
        @Query("memoSize") memoSize: Int? = null,
        @Query("amount") amount: Long? = null,
        @Query("restake") restake: Boolean? = null,
        @Query("lPool") lPool: String? = null,
        @Query("target") target: String? = null,
        @Query("metadataSize") metadataSize: Int? = null,
        @Query("openStatus") openStatus: String? = null
    ): Call<TransferCost>

    @GET("v0/chainParameters")
    fun chainParameters(
    ): Call<ChainParameters>

    @GET("v0/accBalance/{accountAddress}")
    fun accountBalance(@Path("accountAddress") accountAddress: String): Call<AccountBalance>

    @GET("v0/accBalance/{accountAddress}")
    suspend fun accountBalanceSuspended(@Path("accountAddress") accountAddress: String): AccountBalance

    @GET("v1/accTransactions/{accountAddress}")
    fun accountTransactions(
        @Path("accountAddress") accountAddress: String,
        @Query("order") order: String? = null,
        @Query("from") from: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("includeRewards") includeRewards: String? = null
    ): Call<AccountTransactions>

    @PUT("v0/testnetGTUDrop/{accountAddress}")
    fun requestGTUDrop(@Path("accountAddress") accountAddress: String): Call<SubmissionData>

    // Identity Provider
    @GET("v0/ip_info")
    fun getIdentityProviderInfo(): Call<ArrayList<IdentityProvider>>

    @GET("v0/global")
    fun getGlobalInfo(): Call<GlobalParamsWrapper>

    @GET("v0/global")
    suspend fun getGlobalInfoSuspended(): GlobalParamsWrapper

    @GET("v0/request_id")
    fun requestIdentity(@Query("id_request") idRequest: String): Call<IdentityContainer>

    @GET("v0/accEncryptionKey/{accountAddress}")
    fun getAccountEncryptedKey(@Path("accountAddress") accountAddress: String): Call<AccountKeyData>
}
