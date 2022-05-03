package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakerPoolInfo
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_CLOSED_FOR_ALL
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_CLOSED_FOR_NEW
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_OPEN_FOR_ALL
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.uicore.view.SegmentedControlView
import kotlinx.android.synthetic.main.activity_baker_update_pool_settings.*

class BakerUpdatePoolSettingsActivity :
    BaseDelegationBakerActivity(R.layout.activity_baker_update_pool_settings, R.string.baker_update_pool_settings_title) {

    override fun initViews() {
        super.initViews()

        viewModel.bakerDelegationData.oldOpenStatus = viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.openStatus

        viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.let {
            viewModel.selectOpenStatus(it)
        }

        pool_options.clearAll()

        pool_options.addControl(
            getString(R.string.baker_update_pool_settings_option_open),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_OPEN_FOR_ALL))
                }
            }, viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.openStatus == OPEN_STATUS_OPEN_FOR_ALL)
        if (viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.openStatus == OPEN_STATUS_OPEN_FOR_ALL) {
            pool_options.addControl(
                getString(R.string.baker_update_pool_settings_option_close_for_new),
                object : SegmentedControlView.OnItemClickListener {
                    override fun onItemClicked() {
                        viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_CLOSED_FOR_NEW))
                    }
                }, viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.openStatus == OPEN_STATUS_CLOSED_FOR_NEW)
        }
        pool_options.addControl(
            getString(R.string.baker_update_pool_settings_option_close),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_CLOSED_FOR_ALL))
                }
            }, viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.openStatus == OPEN_STATUS_CLOSED_FOR_ALL)

        pool_settings_current_status.text = if (viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.openStatus == OPEN_STATUS_OPEN_FOR_ALL) getString(R.string.baker_update_pool_settings_current_status_open) else getString(R.string.baker_update_pool_settings_current_status_closed)

        update_pool_settings_continue.setOnClickListener {
            gotoBakerRegistration()
        }
    }

    private fun gotoBakerRegistration() {
        val intent = if (viewModel.bakerDelegationData.bakerPoolInfo?.openStatus == OPEN_STATUS_OPEN_FOR_ALL) {
            Intent(this, BakerRegistrationOpenActivity::class.java)
        } else {
            Intent(this, BakerRegistrationConfirmationActivity::class.java)
        }
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun transactionSuccessLiveData() {
    }

    override fun errorLiveData(value: Int) {
    }

    override fun showDetailedLiveData(value: Boolean) {
    }
}
