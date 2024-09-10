package com.concordium.wallet.data.repository

import com.concordium.wallet.onboarding.data.datasource.SharedPreferencesDataSource

class AuthenticationRepository(
    private val sharedPreferences: SharedPreferencesDataSource
) {
    fun saveSeedPhase(seedPhase: String) = runCatching {
        sharedPreferences.seedPhrase = seedPhase
    }

    fun getSeedPhase() = runCatching {
        checkNotNull(sharedPreferences.seedPhrase.takeIf(String::isNotBlank)) {
            "The phrase must not be blank"
        }
    }

    fun saveSeed(seed: String) = runCatching {
        sharedPreferences.seed = seed
    }

    fun getSeed() = runCatching {
        checkNotNull(sharedPreferences.seed.takeIf(String::isNotBlank)) {
            "The seed must not be blank"
        }
    }
}
