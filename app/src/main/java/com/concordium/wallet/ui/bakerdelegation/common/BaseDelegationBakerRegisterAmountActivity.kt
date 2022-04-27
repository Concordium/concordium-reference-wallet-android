package com.concordium.wallet.ui.bakerdelegation.common

import com.concordium.wallet.R
import com.concordium.wallet.uicore.view.SegmentedControlView
import kotlinx.android.synthetic.main.activity_baker_registration_amount.restake_options

abstract class BaseDelegationBakerRegisterAmountActivity(layout: Int, titleId: Int = R.string.app_name) :
    BaseDelegationBakerActivity(layout, titleId) {

    protected var fee: Long? = null

    override fun initViews() {
        super.initViews()

        restake_options.clearAll()
        restake_options.addControl(
            getString(R.string.delegation_register_delegation_yes_restake),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.markRestake(true)
                }
            },viewModel.bakerDelegationData.account?.accountDelegation?.restakeEarnings == true)
        restake_options.addControl(
            getString(R.string.delegation_register_delegation_no_restake),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.markRestake(false)
                }
            },viewModel.bakerDelegationData.account?.accountDelegation?.restakeEarnings != true)
    }

    abstract fun getStakeAmountInputValidator(): StakeAmountInputValidator
}