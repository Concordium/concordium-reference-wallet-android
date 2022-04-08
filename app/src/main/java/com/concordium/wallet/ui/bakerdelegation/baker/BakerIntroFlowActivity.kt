package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.ui.identity.identitycreate.IdentityCreateActivity

class BakerIntroFlowActivity :
    GenericFlowActivity(R.string.baker_intro_flow_title) {

    companion object {
        val TITLES = intArrayOf(R.string.baker_intro_flow_title,
            R.string.delegation_intro_subtitle2,
            R.string.delegation_intro_subtitle3,
            R.string.delegation_intro_subtitle4,
            R.string.delegation_intro_subtitle5,
            R.string.delegation_intro_subtitle6,
            R.string.delegation_intro_subtitle7)
            const val MAX_PAGES = 7
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
        return "file:///android_asset/delegation_intro_flow_en_"+(position+1)+".html"
    }
}
