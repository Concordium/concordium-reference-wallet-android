package com.concordium.wallet.onboarding.data.datasource

interface SharedPreferencesDataSource {
    var termsAndConditionsVersionAccepted: String

    var seedPhrase: String

    var seed: String
}
