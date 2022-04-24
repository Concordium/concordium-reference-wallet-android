package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.common.StatusActivity
import com.concordium.wallet.ui.common.GenericFlowActivity
import kotlinx.android.synthetic.main.delegationbaker_status.*

class BakerStatusActivity :
    StatusActivity(R.string.baker_status_title) {

    override fun initView() {
        status_button_bottom.visibility = View.VISIBLE
        status_button_bottom.text = getString(R.string.baker_status_register_baker)

        if (viewModel.delegationData.account?.isBaking() == true) {

        } else {
            setEmptyState(getString(R.string.baker_status_no_baker))
            status_button_bottom.setOnClickListener {
                continueToBakerAmount()
            }
        }
    }

    private fun continueToBakerAmount() {
        val intent = Intent(this, BakerRegisterAmountActivity::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
