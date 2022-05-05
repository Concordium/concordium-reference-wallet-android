package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakerPoolInfo
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_CLOSED_FOR_ALL
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_OPEN_FOR_ALL
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.uicore.view.SegmentedControlView
import kotlinx.android.synthetic.main.activity_baker_registration.*

class BakerRegistrationActivity :
    BaseDelegationBakerActivity(R.layout.activity_baker_registration, R.string.baker_registration_title) {

    override fun initViews() {
        baker_options.clearAll()
        baker_options.addControl(
            getString(R.string.baker_registration_open),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_OPEN_FOR_ALL))
                }
            }, viewModel.bakerDelegationData.bakerPoolStatus?.poolInfo?.openStatus == OPEN_STATUS_OPEN_FOR_ALL || viewModel.bakerDelegationData.bakerPoolStatus?.poolInfo?.openStatus == null)
        baker_options.addControl(
            getString(R.string.baker_registration_close),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_CLOSED_FOR_ALL))
                }
            }, viewModel.bakerDelegationData.bakerPoolStatus?.poolInfo?.openStatus == OPEN_STATUS_CLOSED_FOR_ALL)

        baker_registration_continue.setOnClickListener {
            onContinueClicked()
        }
    }

    private fun onContinueClicked() {
        val intent = if (viewModel.bakerDelegationData.bakerPoolInfo?.openStatus == OPEN_STATUS_CLOSED_FOR_ALL)
            Intent(this, BakerRegistrationCloseActivity::class.java)
        else
            Intent(this, BakerRegistrationOpenActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun errorLiveData(value: Int) {
    }
}