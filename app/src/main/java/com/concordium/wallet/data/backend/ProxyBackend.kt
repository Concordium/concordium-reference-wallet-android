package com.concordium.wallet.data.backend

import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.model.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ProxyBackend {

    @PUT("v0/submitCredential")
    fun submitCredential(@Body credential: CredentialWrapper): Call<SubmissionData>

    @GET("v0/bakerPool/{poolId}")
    fun bakerPool(@Path("poolId") poolId: String): Call<BakerPoolStatus>

    @GET("v0/bakerPool/{poolId}")
    suspend fun bakerPoolSuspended(@Path("poolId") poolId: String): Response<BakerPoolStatus>

    @GET("v1/appSettings")
    fun appSettings(@Query("platform") platform: String, @Query("appVersion") version: Int): Call<AppSettings>

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
        @Query("type") type: String? = null,
        @Query("memoSize") memoSize: Int? = null,
        @Query("amount") amount: Long? = null,
        @Query("restake") restake: Boolean? = null,
        @Query("lPool") lPool: String? = null,
        @Query("target") target: String? = null,
        @Query("metadataSize") metadataSize: Int? = null,
        @Query("openStatus") openStatus: String? = null,
        @Query("sender") sender: String? = null,
        @Query("contractIndex") contractIndex: Int? = null,
        @Query("contractSubindex") contractSubindex: Int? = null,
        @Query("receiveName") receiveName: String? = null,
        @Query("parameter") parameter: String? = null,
        @Query("executionNRGBuffer") executionNRGBuffer: Int? = null
    ): Call<TransferCost>

    @GET("v0/chainParameters")
    fun chainParameters(): Call<ChainParameters>

    @GET("v0/chainParameters")
    suspend fun chainParametersSuspended(): Response<ChainParameters>

    @GET("v0/passiveDelegation")
    suspend fun passiveDelegationSuspended(): Response<PassiveDelegation>

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
    @GET("v1/ip_info")
    fun getIdentityProviderInfo(): Call<ArrayList<IdentityProvider>>

    @GET("v0/global")
    fun getGlobalInfo(): Call<GlobalParamsWrapper>

    @GET("v0/request_id")
    fun requestIdentity(@Query("id_request") idRequest: String): Call<IdentityContainer>

    @GET("v0/accEncryptionKey/{accountAddress}")
    fun getAccountEncryptedKey(@Path("accountAddress") accountAddress: String): Call<AccountKeyData>

    @GET("v0/CIS2Tokens/{index}/{subIndex}")
    fun cis2Tokens(
        @Path("index") index: String,
        @Path("subIndex") subIndex: String,
        @Query("from") from: Int? = null,
        @Query("limit") limit: Int? = null
    ): Call<CIS2Tokens>
}
