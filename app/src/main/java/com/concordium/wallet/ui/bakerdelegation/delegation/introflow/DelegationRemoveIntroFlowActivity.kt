package com.concordium.wallet.ui.bakerdelegation.delegation.introflow

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationRemoveActivity

class DelegationRemoveIntroFlowActivity :
    BaseDelegationBakerFlowActivity(R.string.delegation_intro_flow_title) {

    override fun getTitles(): IntArray {
        return intArrayOf(R.string.delegation_remove_subtitle1,
            R.string.delegation_remove_subtitle2)
    }

    override fun gotoContinue() {
        val intent = Intent(this, DelegationRemoveActivity::class.java)
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun getLink(position: Int): String {
        return "file:///android_asset/delegation_remove_flow_en_"+(position+1)+".html"
    }
}
