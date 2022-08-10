package com.concordium.wallet.ui.identity.identitycreate

import android.content.Intent
import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity

class IdentityIntroFlow : GenericFlowActivity(R.string.identity_intro_flow_title) {
    companion object {
        val TITLES = intArrayOf(R.string.identity_intro_flow_subtitle1,
            R.string.identity_intro_flow_subtitle2)
        const val MAX_PAGES = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showProgressLine = true
        updateViews()
    }

    override fun gotoContinue() {
        finish()
        val intent = Intent(this, IdentityProviderListActivity::class.java)
        intent.putExtra(IdentityProviderListActivity.SHOW_FOR_FIRST_IDENTITY, true)
        startActivity(intent)
    }

    override fun getMaxPages(): Int {
        return MAX_PAGES
    }

    override fun getPageTitle(position: Int): Int {
        return TITLES[position]
    }

    override fun getLink(position: Int): String {
        return "file:///android_asset/identity_intro_flow_en_" + (position + 1) + ".html"
    }
}
