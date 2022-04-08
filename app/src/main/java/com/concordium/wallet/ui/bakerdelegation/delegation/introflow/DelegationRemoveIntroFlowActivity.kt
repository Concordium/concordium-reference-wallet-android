package com.concordium.wallet.ui.bakerdelegation.delegation.introflow

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationRegisterPoolActivity

class DelegationRemoveIntroFlowActivity :
    BaseDelegationBakerFlowActivity(R.string.delegation_intro_flow_title) {

    override fun getTitles(): IntArray {
        return intArrayOf(R.string.delegation_remove_subtitle1,
            R.string.delegation_remove_subtitle2)
    }

    override fun gotoContinue() {
        val intent = Intent(this, DelegationRegisterPoolActivity::class.java)
        intent.putExtra(BaseDelegationBakerFlowActivity.EXTRA_DELEGATION_DATA, delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun getLink(position: Int): String {
        return "file:///android_asset/delegation_remove_flow_en_"+(position+1)+".html"
    }

}
