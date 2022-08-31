package com.concordium.wallet.ui.passphrase.recoverprocess.retrofit

import com.concordium.wallet.data.model.IdentityTokenContainer
import com.concordium.wallet.data.model.RecoverResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface IdentityProviderApi {
    @GET suspend fun recover(@Url url: String?): Response<RecoverResponse>
    @GET suspend fun identity(@Url url: String?): Response<IdentityTokenContainer>
}
