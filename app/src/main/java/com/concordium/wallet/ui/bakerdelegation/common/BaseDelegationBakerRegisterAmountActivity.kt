package com.concordium.wallet.ui.bakerdelegation.common

import android.widget.TextView
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.uicore.view.AmountEditText
import com.concordium.wallet.uicore.view.SegmentedControlView
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormatSymbols

abstract class BaseDelegationBakerRegisterAmountActivity : BaseDelegationBakerActivity() {
    protected var validateFee: BigInteger? = null
    protected var baseDelegationBakerRegisterAmountListener: BaseDelegationBakerRegisterAmountListener? =
        null

    interface BaseDelegationBakerRegisterAmountListener {
        fun onReStakeChanged()
    }

    protected fun initReStakeOptionsView(reStakeOptions: SegmentedControlView) {
        val initiallyReStake = if (viewModel.bakerDelegationData.isBakerFlow()) {
            viewModel.bakerDelegationData.account?.accountBaker?.restakeEarnings == true || viewModel.bakerDelegationData.account?.accountBaker?.restakeEarnings == null
        } else {
            viewModel.bakerDelegationData.account?.accountDelegation?.restakeEarnings == true || viewModel.bakerDelegationData.account?.accountDelegation?.restakeEarnings == null
        }
        viewModel.bakerDelegationData.restake = initiallyReStake

        reStakeOptions.clearAll()
        reStakeOptions.addControl(
            getString(R.string.delegation_register_delegation_yes_restake),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.markRestake(true)
                    baseDelegationBakerRegisterAmountListener?.onReStakeChanged()
                }
            }, initiallyReStake
        )
        reStakeOptions.addControl(
            getString(R.string.delegation_register_delegation_no_restake),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.markRestake(false)
                    baseDelegationBakerRegisterAmountListener?.onReStakeChanged()
                }
            }, !initiallyReStake
        )
    }

    protected fun moreThan95Percent(amountToStake: BigInteger): Boolean {
        return amountToStake.toBigDecimal() > (viewModel.bakerDelegationData.account?.finalizedBalance ?: BigInteger.ZERO).toBigDecimal() * BigDecimal(0.95)
    }

    protected fun validateAmountInput(amount: AmountEditText, amountError: TextView) {
        if (amount.text.isNotEmpty() && !amount.text.startsWith("Ͼ")) {
            amount.setText("Ͼ".plus(amount.text.toString()))
            amount.setSelection(amount.text.length)
        }
        setAmountHint(amount)
        if (amount.text.toString().isNotBlank() && amount.text.toString() != "Ͼ") {
            val stakeAmountInputValidator = getStakeAmountInputValidator()
            val stakeError = stakeAmountInputValidator.validate(
                CurrencyUtil.toGTUValue(amount.text.toString())?.toString(), validateFee
            )
            if (stakeError != StakeAmountInputValidator.StakeError.OK) {
                amountError.text = stakeAmountInputValidator.getErrorText(this, stakeError)
                showError(stakeError)
            } else {
                hideError()
                loadTransactionFee()
            }
        } else {
            hideError()
        }
    }

    protected fun setAmountHint(amount: AmountEditText) {
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
