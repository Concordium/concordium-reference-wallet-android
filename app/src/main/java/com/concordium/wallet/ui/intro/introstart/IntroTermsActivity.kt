package com.concordium.wallet.ui.intro.introstart

import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Bundle
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_intro_terms.*


class IntroTermsActivity : BaseActivity(R.layout.activity_intro_terms, R.string.terms_title) {


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    // endregion

    //region Initialize
    //************************************************************



    private fun initViews() {
        confirm_button.setOnClickListener {
            gotoStart()
        }

        var html = getString(R.string.terms_text)

        info_webview.setVerticalScrollBarEnabled(false)
        info_webview.loadData(html, "text/html", "UTF-8")

    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoStart() {
        val oldHash = App.appCore.session.setTermsHashed(App.appContext.getString(R.string.terms_text).hashCode())

        finish()
        val intent = if(App.appCore.session.hasSetupPassword) Intent(this, AuthLoginActivity::class.java) else Intent(this, IntroStartActivity::class.java)
        startActivity(intent)

    }

    override fun loggedOut() {
        // do nothing as we are one of the root activities
    }

    //endregion

}
