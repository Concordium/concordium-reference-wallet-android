package com.concordium.wallet.ui.bakerdelegation.baker.introflow

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.baker.BakerStatusActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel

class BakerRegistrationIntroFlow :
    BaseDelegationBakerFlowActivity(R.string.baker_intro_flow_title) {

    override fun getTitles(): IntArray {
        return intArrayOf(R.string.baker_intro_subtitle1,
            R.string.baker_intro_subtitle2,
            R.string.baker_intro_subtitle3)
    }

    override fun gotoContinue() {
        val intent = Intent(this, BakerStatusActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, delegationData)
        startActivity(intent)
    }

    override fun getLink(position: Int): String {
        return "file:///android_asset/baker_intro_flow_en_"+(position+1)+".html"
    }
}
