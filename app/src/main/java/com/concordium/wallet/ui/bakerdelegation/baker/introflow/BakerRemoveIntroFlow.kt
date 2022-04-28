package com.concordium.wallet.ui.bakerdelegation.baker.introflow

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.baker.BakerRegistrationConfirmationActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel

class BakerRemoveIntroFlow :
    BaseDelegationBakerFlowActivity(R.string.baker_remove_intro_flow_title) {

    override fun getTitles(): IntArray {
        return intArrayOf(R.string.baker_remove_intro_subtitle1)
    }

    override fun gotoContinue() {
        val intent = Intent(this, BakerRegistrationConfirmationActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, delegationData)
        startActivity(intent)
    }

    override fun getLink(position: Int): String {
        return "file:///android_asset/baker_remove_intro_flow_en_"+(position+1)+".html"
    }
}
