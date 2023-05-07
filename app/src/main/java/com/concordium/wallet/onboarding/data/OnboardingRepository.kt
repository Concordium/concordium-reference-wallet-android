package com.concordium.wallet.onboarding.data

import com.concordium.wallet.onboarding.data.datasource.OnboardingService
import com.concordium.wallet.onboarding.data.datasource.SharedPreferencesDataSource
import com.concordium.wallet.onboarding.ui.terms.model.toItem

class OnboardingRepository(
    private val sharedPreferences: SharedPreferencesDataSource,
    private val onboardingService: OnboardingService
) {

    suspend fun saveAcceptedTermsAndConditionsVersion(version: String) = runCatching {
        sharedPreferences.termsAndConditionsVersionAccepted = version
    }

    suspend fun getLocalAcceptedTermsAndConditionsVersion(): String =
        sharedPreferences.termsAndConditionsVersionAccepted

    suspend fun getRemoteAcceptedTermsAndConditionsVersion() = runCatching {
        onboardingService.getTermsAndConditions()
    }.map { it.toItem() }
}
