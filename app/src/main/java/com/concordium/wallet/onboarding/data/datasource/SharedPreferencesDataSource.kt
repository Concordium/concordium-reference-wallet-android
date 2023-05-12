package com.concordium.wallet.onboarding.data.datasource

interface SharedPreferencesDataSource {
    var termsAndConditionsVersionAccepted: String

    var seedPhase: String
}
