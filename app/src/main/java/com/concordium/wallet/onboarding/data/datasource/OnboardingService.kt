package com.concordium.wallet.onboarding.data.datasource

import com.concordium.wallet.onboarding.data.model.TermsAndConditionsDto
import retrofit2.http.GET

interface OnboardingService {

    @GET("v0/termsAndConditionsVersion")
    suspend fun getTermsAndConditions(): TermsAndConditionsDto
}