package com.concordium.wallet.ui.cis2.retrofit

import com.concordium.wallet.data.model.TokenMetadata
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface MetadataApi {
    @GET suspend fun metadata(@Url url: String?): Response<TokenMetadata>
}
