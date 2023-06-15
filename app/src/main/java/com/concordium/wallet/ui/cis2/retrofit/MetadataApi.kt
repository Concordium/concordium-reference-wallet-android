package com.concordium.wallet.ui.cis2.retrofit

import com.concordium.wallet.data.model.TokenMetadata
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url
import retrofit2.http.Query

interface MetadataApi {
    /**
     * @param metadataChecksum is a special parameter only used by the interceptor to ensure the response body has that checksum
     */
    @GET suspend fun metadata(@Url url: String?, @Query(METADATA_CHECKSUM_FAKE_PARAMETER) metadataChecksum: String?): Response<TokenMetadata>
}
