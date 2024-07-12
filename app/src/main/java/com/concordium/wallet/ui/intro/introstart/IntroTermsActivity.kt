package com.concordium.wallet.ui.intro.introstart

import android.annotation.SuppressLint
import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseActivity

class IntroTermsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro_terms)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
    }

    override fun loggedOut() {
        // do nothing as we are one of the root activities
    }
}
