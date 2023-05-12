package com.concordium.wallet.data.repository

import com.concordium.wallet.onboarding.data.datasource.SharedPreferencesDataSource

class AuthenticationRepository(
    private val sharedPreferences: SharedPreferencesDataSource
) {
    fun saveSeedPhase(seedPhase: String) = runCatching {
        sharedPreferences.seedPhase = seedPhase
    }

    fun getSeedPhase() = runCatching {
        sharedPreferences.seedPhase
    }
}