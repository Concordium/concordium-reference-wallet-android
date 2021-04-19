package com.concordium.wallet.data.backend.repository

import com.concordium.wallet.App
import com.concordium.wallet.core.backend.BackendCallback
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.IdentityContainer
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.data.model.IdentityRequest

class IdentityProviderRepository() {

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

        return BackendRequest<ArrayList<IdentityProvider>>(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getGlobalInfoSuspended() = backend.getGlobalInfoSuspended()

    fun getIGlobalInfo(
        success: (GlobalParamsWrapper) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<GlobalParamsWrapper> {
        val call = backend.getGlobalInfo()
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

        return BackendRequest<IdentityContainer>(
            call = call,
            success = success,
            failure = failure
        )
    }

}