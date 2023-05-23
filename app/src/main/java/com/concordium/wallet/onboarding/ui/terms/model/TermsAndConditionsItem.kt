package com.concordium.wallet.onboarding.ui.terms.model

import com.concordium.wallet.onboarding.data.model.TermsAndConditionsDto

class TermsAndConditionsItem(
    val url: String,
    val version: String
)

fun TermsAndConditionsDto.toItem() = TermsAndConditionsItem(url = url, version = version)
