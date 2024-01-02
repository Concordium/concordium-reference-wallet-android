package com.concordium.wallet.data.backend.repository

import com.concordium.wallet.App
import com.concordium.wallet.core.backend.BackendCallback
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.IdentityContainer
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.data.model.IdentityRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

import com.concordium.sdk.Connection
import com.concordium.sdk.ClientV2
import com.concordium.sdk.requests.BlockQuery
import com.concordium.sdk.TLSConfig

class IdentityProviderRepository {
    private val gson = App.appCore.gson
    private val backend = App.appCore.getProxyBackend()

    fun getIdentityProviderInfo(
        success: (ArrayList<IdentityProvider>) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<ArrayList<IdentityProvider>> {
        val call = backend.getIdentityProviderInfo()
        call.enqueue(object : BackendCallback<ArrayList<IdentityProvider>>() {
            override fun onResponseData(response: ArrayList<IdentityProvider>) {
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
        val connection = Connection.newBuilder()
                .host("grpc.testnet.concordium.com")
                .port(20000)
                .useTLS(TLSConfig.auto())
                .build()
        val client = ClientV2.from(connection)

        val call = backend.getGlobalInfo()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val globalInfo = client.getCryptographicParameters(BlockQuery.BEST)
                System.out.println(globalInfo.getOnChainCommitmentKey().toHex())
                System.out.println(globalInfo.getBulletproofGenerators().toHex())
                System.out.println(globalInfo.getGenesisString())
                System.out.println(globalInfo.getVersion())
                success(GlobalParamsWrapper( v = globalInfo.getVersion(), value = GlobalParams(onChainCommitmentKey= globalInfo.getOnChainCommitmentKey().toHex(), bulletproofGenerators = globalInfo.getBulletproofGenerators().toHex(), genesisString= globalInfo.getGenesisString())))
            } catch (t: Throwable) {
                failure?.invoke(t)
            }
        }

        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun requestIdentity(
        request: IdentityRequest,
        success: (IdentityContainer) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<IdentityContainer> {
        val json = gson.toJson(
            IdentityRequest(
                request.idObjectRequest
            )
        )
        val call = backend.requestIdentity(json)
        call.enqueue(object : BackendCallback<IdentityContainer>() {
            override fun onResponseData(response: IdentityContainer) {
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
}