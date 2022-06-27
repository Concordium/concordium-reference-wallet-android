package com.concordium.wallet.ui.bakerdelegation.common

import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.uicore.view.SegmentedControlView
import kotlinx.android.synthetic.main.activity_baker_registration_amount.*
import java.text.DecimalFormatSymbols

abstract class BaseDelegationBakerRegisterAmountActivity(layout: Int, titleId: Int = R.string.app_name) :
    BaseDelegationBakerActivity(layout, titleId) {

    protected var validateFee: Long? = null
    protected var baseDelegationBakerRegisterAmountListener: BaseDelegationBakerRegisterAmountListener? = null

    interface BaseDelegationBakerRegisterAmountListener {
        fun onRestakeChanged()
    }

    override fun initViews() {
        super.initViews()

        val initiallyRestake = if (viewModel.bakerDelegationData.isBakerFlow()) {
            viewModel.bakerDelegationData.account?.accountBaker?.restakeEarnings == true || viewModel.bakerDelegationData.account?.accountBaker?.restakeEarnings == null
        } else {
            viewModel.bakerDelegationData.account?.accountDelegation?.restakeEarnings == true || viewModel.bakerDelegationData.account?.accountDelegation?.restakeEarnings == null
        }
        viewModel.bakerDelegationData.restake = initiallyRestake

        restake_options.clearAll()
        restake_options.addControl(
            getString(R.string.delegation_register_delegation_yes_restake),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.markRestake(true)
                    baseDelegationBakerRegisterAmountListener?.onRestakeChanged()
                }
            }, initiallyRestake)
        restake_options.addControl(
            getString(R.string.delegation_register_delegation_no_restake),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.markRestake(false)
                    baseDelegationBakerRegisterAmountListener?.onRestakeChanged()
                }
            }, !initiallyRestake)
    }

    protected fun moreThan95Percent(amountToStake: Long): Boolean {
        return amountToStake > (viewModel.bakerDelegationData.account?.finalizedBalance ?: 0) * 0.95
    }

    protected fun validateAmountInput() {
        if (amount.text.isNotEmpty() && !amount.text.startsWith("Ͼ")) {
            amount.setText("Ͼ".plus(amount.text.toString()))
            amount.setSelection(amount.text.length)
        }
        setAmountHint()
        if (amount.text.toString().isNotBlank() && amount.text.toString() != "Ͼ") {
            val stakeAmountInputValidator = getStakeAmountInputValidator()
            val stakeError = stakeAmountInputValidator.validate(CurrencyUtil.toGTUValue(amount.text.toString())?.toString(), validateFee)
            if (stakeError != StakeAmountInputValidator.StakeError.OK) {
                amount_error.text = stakeAmountInputValidator.getErrorText(this, stakeError)
                showError(stakeError)
            } else {
                hideError()
                loadTransactionFee()
            }
        } else {
            hideError()
        }
    }

    protected fun setAmountHint() {
        when {
            amount.text.isNotEmpty() -> {
                amount.hint = ""
            }
            else -> {
                amount.hint = "Ͼ0" + DecimalFormatSymbols.getInstance().decimalSeparator + "00"
            }
        }
    }

    abstract fun getStakeAmountInputValidator(): StakeAmountInputValidator

    abstract fun showError(stakeError: StakeAmountInputValidator.StakeError?)
    abstract fun hideError()
    abstract fun loadTransactionFee()
}
