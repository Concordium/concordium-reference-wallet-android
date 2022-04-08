package com.concordium.wallet.ui.intro.introflow

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.ui.identity.identitycreate.IdentityCreateActivity

class IntroFlowActivity :
    GenericFlowActivity(R.string.intro_flow_title) {

    companion object {
        val TITLES = intArrayOf(R.string.intro_start_create_intro_subtitle1,R.string.intro_start_create_intro_subtitle2,R.string.intro_start_create_intro_subtitle3,R.string.intro_start_create_intro_subtitle4,R.string.intro_start_create_intro_subtitle5)
        const val MAX_PAGES = 5
    }

    override fun gotoContinue() {
        val intent = Intent(this, IdentityCreateActivity::class.java)
        startActivity(intent)
    }

    override fun getMaxPages(): Int {
        return MAX_PAGES
    }

    override fun getPageTitle(position: Int): Int {
        return TITLES[position]
    }

    override fun getLink(position: Int): String {
        return "file:///android_asset/intro_flow_onboarding_en_"+(position+1)+".html"
    }

    //"file:///android_asset/intro_flow_onboarding_en_"+(position+1)+".html"
}
