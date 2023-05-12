package com.concordium.wallet.onboarding.data.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import com.walletconnect.util.Empty

class SharedPreferencesDataSourceImpl(private val prefs: SharedPreferences) : SharedPreferencesDataSource {

    companion object {
        private const val TERMS_AND_CONDITIONS_VERSION_ACCEPTED_KEY = "TermsAndConditionsAccepted"
        private const val SEED_PHASE_KEY = "SeedPhase"
    }

    override var termsAndConditionsVersionAccepted: String
        get() = prefs.getString(TERMS_AND_CONDITIONS_VERSION_ACCEPTED_KEY, String.Empty)!!
        set(value) = prefs.edit { putString(TERMS_AND_CONDITIONS_VERSION_ACCEPTED_KEY, value) }

    override var seedPhase: String
        get() = prefs.getString(SEED_PHASE_KEY, String.Empty)!!
        set(value) = prefs.edit { putString(SEED_PHASE_KEY, value) }
}
