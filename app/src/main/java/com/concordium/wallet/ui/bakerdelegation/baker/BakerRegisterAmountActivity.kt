package com.concordium.wallet.ui.bakerdelegation.baker

import android.app.AlertDialog
import android.content.Intent
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerRegisterAmountActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.common.StakeAmountInputValidator
import com.concordium.wallet.ui.common.GenericFlowActivity
import kotlinx.android.synthetic.main.activity_baker_registration_amount.*
import kotlinx.android.synthetic.main.activity_baker_registration_amount.amount
import kotlinx.android.synthetic.main.activity_baker_registration_amount.amount_error
import kotlinx.android.synthetic.main.activity_baker_registration_amount.balance_amount
import kotlinx.android.synthetic.main.activity_baker_registration_amount.pool_estimated_transaction_fee
import java.text.DecimalFormatSymbols

class BakerRegisterAmountActivity :
    BaseDelegationBakerRegisterAmountActivity(R.layout.activity_baker_registration_amount, R.string.baker_registration_amount_title) {

    override fun initViews() {
        super.initViews()

        if (viewModel.isUpdatingBaker())
            setActionBarTitle(R.string.baker_registration_update_amount_title)

        balance_amount.text = CurrencyUtil.formatGTU(viewModel.bakerDelegationData.account?.finalizedBalance ?: 0, true)
        baker_amount.text = CurrencyUtil.formatGTU(viewModel.bakerDelegationData.account?.accountBaker?.stakedAmount ?: "0", true)

        viewModel.transactionFeeLiveData.observe(this, object : Observer<Long> {
            override fun onChanged(value: Long?) {
                value?.let {
                    fee = value
                    pool_estimated_transaction_fee.visibility = View.VISIBLE
                    pool_estimated_transaction_fee.text = getString(
                        R.string.delegation_register_delegation_amount_estimated_transaction_fee, CurrencyUtil.formatGTU(value)
                    )
                }
            }
        })

        viewModel.chainParametersLoadedLiveData.observe(this, Observer { success ->
            success?.let {
                updateViews()
                showWaiting(false)
            }
        })

        showWaiting(true)
        viewModel.loadChainParameters()
    }

    private fun updateViews() {
        amount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onContinueClicked()
            }
            false
        }
        setAmountHint()
        amount.keyListener = DigitsKeyListener.getInstance("0123456789" + DecimalFormatSymbols.getInstance().decimalSeparator)
        amount.doOnTextChanged { text, _, _, _ ->
            if (text != null && (text.toString() == DecimalFormatSymbols.getInstance().decimalSeparator.toString() || text.filter { it == DecimalFormatSymbols.getInstance().decimalSeparator }.length > 1)) {
                amount.setText(text.dropLast(1))
            }
            if (amount.text.isNotEmpty() && !amount.text.startsWith("Ͼ")) {
                amount.setText("Ͼ".plus(amount.text.toString()))
                amount.setSelection(amount.text.length)
            }
            setAmountHint()
            val stakeAmountInputValidator = getStakeAmountInputValidator()
            val stakeError = stakeAmountInputValidator.validate(CurrencyUtil.toGTUValue(amount.text.toString())?.toString(), fee)
            if (stakeError != StakeAmountInputValidator.StakeError.OK) {
                amount_error.text = stakeAmountInputValidator.getErrorText(this, stakeError)
                showError()
            } else {
                hideError()
                viewModel.loadTransactionFee(true)
            }
        }
        amount.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus && amount.text.isEmpty()) amount.hint = ""
            else setAmountHint()
        }

        baker_registration_continue.setOnClickListener {
            onContinueClicked()
        }
    }

    private fun showError() {
        amount.setTextColor(getColor(R.color.text_pink))
        amount_error.visibility = View.VISIBLE
    }

    private fun hideError() {
        amount.setTextColor(getColor(R.color.theme_blue))
        amount_error.visibility = View.INVISIBLE
    }

    override fun getStakeAmountInputValidator(): StakeAmountInputValidator {
        return StakeAmountInputValidator(
            viewModel.bakerDelegationData.chainParameters?.minimumEquityCapital,
            null,
            (viewModel.bakerDelegationData.account?.finalizedBalance ?: 0),
            viewModel.bakerDelegationData.account?.getAtDisosal(),
            viewModel.bakerDelegationData.bakerPoolStatus?.delegatedCapital,
            viewModel.bakerDelegationData.bakerPoolStatus?.delegatedCapitalCap,
            viewModel.bakerDelegationData.account?.accountDelegation?.stakedAmount,
            viewModel.isInCoolDown(),
            viewModel.bakerDelegationData.account?.accountDelegation?.delegationTarget?.bakerId,
            viewModel.bakerDelegationData.poolId)
    }

    private fun setAmountHint() {
        when {
            amount.text.isNotEmpty() -> {
                amount.hint = ""
            }
            else -> {
                amount.hint = "Ͼ0" + DecimalFormatSymbols.getInstance().decimalSeparator + "00"
            }
        }
    }

    override fun transactionSuccessLiveData() {
        TODO("Not yet implemented")
    }

    override fun errorLiveData(value: Int) {
        TODO("Not yet implemented")
    }

    override fun showDetailedLiveData(value: Boolean) {
        TODO("Not yet implemented")
    }

    private fun onContinueClicked() {
        if (!baker_registration_continue.isEnabled) return

        val stakeAmountInputValidator = getStakeAmountInputValidator()
        val stakeError = stakeAmountInputValidator.validate(CurrencyUtil.toGTUValue(amount.text.toString())?.toString(), fee)
        if (stakeError != StakeAmountInputValidator.StakeError.OK) {
            amount_error.text = stakeAmountInputValidator.getErrorText(this, stakeError)
            showError()
            return
        }

        val amountToStake = getAmountToStake()
        if (viewModel.isUpdatingBaker()) {
            /*
            when {
                (amountToStake == viewModel.delegationData.oldStakedAmount &&
                        viewModel.getPoolId() == viewModel.delegationData.oldDelegationTargetPoolId?.toString() ?: "" &&
                        viewModel.delegationData.restake == viewModel.delegationData.oldRestake &&
                        viewModel.delegationData.isBakerPool == viewModel.delegationData.oldDelegationIsBaker) -> showNoChange()
                amountToStake == 0L -> showNewAmountZero()
                amountToStake < viewModel.delegationData.account?.accountDelegation?.stakedAmount?.toLongOrNull() ?: 0 -> showReduceWarning()
                amountToStake > (viewModel.delegationData.account?.finalizedBalance ?: 0) * 0.95 -> show95PercentWarning()
                else -> continueToBakerKeysGeneration()
            }
            */
        } else {
            when {
                amountToStake > (viewModel.bakerDelegationData.account?.finalizedBalance ?: 0) * 0.95 -> show95PercentWarning()
                else -> continueBakerRegistration()
            }
        }
    }

    private fun getAmountToStake(): Long {
        return CurrencyUtil.toGTUValue(amount.text.toString()) ?: 0
    }

    private fun show95PercentWarning() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.baker_more_than_95_title)
        builder.setMessage(getString(R.string.baker_more_than_95_message))
        builder.setPositiveButton(getString(R.string.baker_more_than_95_continue)) { _, _ -> continueBakerRegistration() }
        builder.setNegativeButton(getString(R.string.baker_more_than_95_new_stake)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun continueBakerRegistration() {
        viewModel.bakerDelegationData.amount = CurrencyUtil.toGTUValue(amount.text.toString())
        val intent = Intent(this, BakerRegistrationActivity::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
