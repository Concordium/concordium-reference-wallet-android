package com.concordium.wallet.ui.bakerdelegation.baker

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerRegisterAmountActivity
import com.concordium.wallet.uicore.view.SegmentedControlView
import kotlinx.android.synthetic.main.activity_baker_registration_amount.*
import kotlinx.android.synthetic.main.activity_baker_registration_amount.amount
import kotlinx.android.synthetic.main.activity_baker_registration_amount.balance_amount
import kotlinx.android.synthetic.main.activity_baker_registration_amount.pool_estimated_transaction_fee
import kotlinx.android.synthetic.main.activity_baker_registration_amount.restake_options

class BakerRegisterAmountActivity :
    BaseDelegationBakerRegisterAmountActivity(R.layout.activity_baker_registration_amount, R.string.baker_registration_amount_title) {

    override fun initViews() {
        if (viewModel.isUpdatingBaker())
            setActionBarTitle(R.string.baker_registration_update_amount_title)

        restake_options.clearAll()
        restake_options.addControl(
            getString(R.string.delegation_register_delegation_yes_restake),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.markRestake(true)
                }
            },
            viewModel.delegationData.restake
        )
        restake_options.addControl(
            getString(R.string.delegation_register_delegation_no_restake),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.markRestake(false)
                }
            },
            !viewModel.delegationData.restake
        )

        amount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onContinueClicked()
            }
            false
        }

        balance_amount.text = CurrencyUtil.formatGTU(viewModel.delegationData.account?.finalizedBalance ?: 0, true)
        baker_amount.text = CurrencyUtil.formatGTU(viewModel.delegationData.account?.accountBaker?.stakedAmount ?: "0", true)

        viewModel.transactionFeeLiveData.observe(this, object : Observer<Long> {
            override fun onChanged(value: Long?) {
                value?.let {
                    pool_estimated_transaction_fee.visibility = View.VISIBLE
                    pool_estimated_transaction_fee.text = getString(
                        R.string.delegation_register_delegation_amount_estimated_transaction_fee, CurrencyUtil.formatGTU(value)
                    )
                }
            }
        })

        viewModel.loadTransactionFee(true)
    }

    private fun onContinueClicked() {
    }
}
