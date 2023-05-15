package com.concordium.wallet.ui.bakerdelegation.delegation.introflow

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationRegisterPoolActivity

class DelegationUpdateIntroFlowActivity :
    BaseDelegationBakerFlowActivity(R.string.delegation_intro_flow_title) {

    override fun getTitles(): IntArray {
        return intArrayOf(R.string.delegation_update_subtitle1,
            R.string.delegation_update_subtitle2,
            R.string.delegation_update_subtitle3)
    }

    override fun gotoContinue() {
        val intent = Intent(this, DelegationRegisterPoolActivity::class.java)
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun getLink(position: Int): String {
        return "file:///android_asset/delegation_update_flow_en_"+(position+1)+".html"
    }
}
