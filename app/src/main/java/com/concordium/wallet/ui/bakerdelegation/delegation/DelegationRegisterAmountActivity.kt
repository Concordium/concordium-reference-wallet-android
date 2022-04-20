package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.content.Intent
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.data.model.DelegationData
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.bakerdelegation.common.StakeAmountInputValidator
import com.concordium.wallet.uicore.view.SegmentedControlView
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.*
import java.text.DecimalFormatSymbols

class DelegationRegisterAmountActivity :
    BaseDelegationActivity(R.layout.activity_delegation_registration_amount, R.string.delegation_register_delegation_title) {

    private fun showError(stakeError: StakeAmountInputValidator.StakeError?) {
        amount.setTextColor(getColor(R.color.text_pink))
        amount_error.visibility = View.VISIBLE
        if (stakeError == StakeAmountInputValidator.StakeError.POOL_LIMIT_REACHED) {
            pool_limit_title.setTextColor(getColor(R.color.text_pink))
            pool_limit.setTextColor(getColor(R.color.text_pink))
        } else {
            pool_limit_title.setTextColor(getColor(R.color.text_black))
            pool_limit.setTextColor(getColor(R.color.text_black))
        }
    }

    private fun hideError() {
        amount.setTextColor(getColor(R.color.theme_blue))
        pool_limit_title.setTextColor(getColor(R.color.text_black))
        pool_limit.setTextColor(getColor(R.color.text_black))
        amount_error.visibility = View.INVISIBLE
    }

    private fun showConfirmationPage() {
    }

    override fun initViews() {
        if (viewModel.isUpdating())
            setActionBarTitle(R.string.delegation_update_delegation_title)

        restake_options.clearAll()
        restake_options.addControl(getString(R.string.delegation_register_delegation_yes_restake), object: SegmentedControlView.OnItemClickListener {
            override fun onItemClicked(){
                viewModel.markRestake(true)
            }
        }, viewModel.delegationData.restake)
        restake_options.addControl(getString(R.string.delegation_register_delegation_no_restake), object: SegmentedControlView.OnItemClickListener {
            override fun onItemClicked(){
                viewModel.markRestake(false)
            }
        }, !viewModel.delegationData.restake)

        amount.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onContinueClicked()
                    true
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
            val stakeAmountInputValidator = StakeAmountInputValidator(
                if (viewModel.isUpdating()) "0" else "1",
                null,
                (viewModel.delegationData.account?.finalizedBalance ?: 0).toString(),
                viewModel.delegationData.bakerPoolStatus?.delegatedCapital,
                viewModel.delegationData.bakerPoolStatus?.delegatedCapitalCap,
                viewModel.delegationData.account?.accountDelegation?.stakedAmount)
            val stakeError = stakeAmountInputValidator.validate(CurrencyUtil.toGTUValue(amount.text.toString())?.toString())
            if (stakeError != StakeAmountInputValidator.StakeError.OK) {
                amount_error.text = stakeAmountInputValidator.getErrorText(this, stakeError)
                showError(stakeError)
            } else {
                hideError()
                viewModel.loadTransactionFee(true)
            }
            if (viewModel.isInCoolDown()) {
                pool_registration_continue.isEnabled = getAmountToStake() > viewModel.delegationData.oldStakedAmount ?: 0
            } else {
                pool_registration_continue.isEnabled = true
            }
        }
        amount.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus && amount.text.isEmpty()) amount.hint = ""
            else setAmountHint()
        }

        pool_registration_continue.setOnClickListener {
            onContinueClicked()
        }

        amount_locked.setOnClickListener {
            amount_locked.visibility = View.GONE
            amount.isEnabled = true
        }

        balance_amount.text = CurrencyUtil.formatGTU(viewModel.delegationData.account?.finalizedBalance ?: 0, true)
        delegation_amount.text = CurrencyUtil.formatGTU(0, true)
        viewModel.delegationData.account?.let { account ->
            account.accountDelegation?.let { accountDelegation ->
                delegation_amount.text = CurrencyUtil.formatGTU(accountDelegation.stakedAmount, true)
            }
        }

        pool_limit.text =
            viewModel.delegationData.bakerPoolStatus?.let {
                CurrencyUtil.formatGTU(it.delegatedCapitalCap, true)
            }
        current_pool.text =
            viewModel.delegationData.bakerPoolStatus?.let {
                CurrencyUtil.formatGTU(it.delegatedCapital, true)
            }

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

        pool_info.visibility = if (viewModel.delegationData.isLPool) View.GONE else View.VISIBLE

        updateContent()

        initializeWaitingLiveData()
        initializeShowDetailedLiveData()
    }

    override fun transactionSuccessLiveData() {
    }

    override fun errorLiveData(value: Int) {
        showError(null)
    }

    override fun showDetailedLiveData(value: Boolean) {
        if (value) {
            showConfirmationPage()
        }
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

    private fun updateContent() {
        if (viewModel.isInCoolDown()) {
            amount_locked.visibility = View.VISIBLE
            amount.isEnabled = false
            pool_registration_continue.isEnabled = false
        }
        if (viewModel.delegationData.type == DelegationData.TYPE_UPDATE_DELEGATION) {
            viewModel.delegationData.oldStakedAmount = viewModel.delegationData.account?.accountDelegation?.stakedAmount?.toLong() ?: 0
            amount_desc.text = getString(R.string.delegation_update_delegation_amount_enter_amount)
            amount.setText(viewModel.delegationData.account?.accountDelegation?.stakedAmount?.let { CurrencyUtil.formatGTU(it,false) })
        }
    }

    private fun onContinueClicked() {

        val stakeAmountInputValidator = StakeAmountInputValidator(
            if (viewModel.isUpdating()) "0" else "1",
            null,
            (viewModel.delegationData.account?.finalizedBalance ?: 0).toString(),
            viewModel.delegationData.bakerPoolStatus?.delegatedCapital,
            viewModel.delegationData.bakerPoolStatus?.delegatedCapitalCap,
            viewModel.delegationData.account?.accountDelegation?.stakedAmount)

        val stakeError = stakeAmountInputValidator.validate(CurrencyUtil.toGTUValue(amount.text.toString())?.toString())
        if (stakeError != StakeAmountInputValidator.StakeError.OK) {
            amount_error.text = stakeAmountInputValidator.getErrorText(this, stakeError)
            showError(stakeError)
            return
        }

        val amountToStake = getAmountToStake()
        if (viewModel.isUpdating()) {
            when {
                (amountToStake == viewModel.delegationData.oldStakedAmount &&
                    viewModel.getPoolId() == viewModel.delegationData.oldDelegationTargetPoolId?.toString() ?: "" &&
                    viewModel.delegationData.restake == viewModel.delegationData.oldRestake &&
                    viewModel.delegationData.isBakerPool == viewModel.delegationData.oldDelegationIsBaker) -> showNoChange()
                amountToStake == 0L -> showNewAmountZero()
                amountToStake < viewModel.delegationData.account?.accountDelegation?.stakedAmount?.toLongOrNull() ?: 0 -> showReduceWarning()
                amountToStake > (viewModel.delegationData.account?.finalizedBalance ?: 0) * 0.95 -> show95PercentWarning()
                else -> continueToConfirmation()
            }
        } else {
            when {
                amountToStake > (viewModel.delegationData.account?.finalizedBalance ?: 0) * 0.95 -> show95PercentWarning()
                else -> continueToConfirmation()
            }
        }
    }

    private fun getAmountToStake(): Long {
        return CurrencyUtil.toGTUValue(amount.text.toString()) ?: 0
    }

    private fun showNoChange() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_no_changes_title)
        builder.setMessage(getString(R.string.delegation_no_changes_message))
        builder.setPositiveButton(getString(R.string.delegation_no_changes_ok)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun showNewAmountZero() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_amount_zero_title)
        builder.setMessage(getString(R.string.delegation_amount_zero_message))
        builder.setPositiveButton(getString(R.string.delegation_amount_zero_continue)) { _, _ -> continueToConfirmation() }
        builder.setNegativeButton(getString(R.string.delegation_amount_zero_new_stake)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun showReduceWarning() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_register_delegation_reduce_warning_title)
        builder.setMessage(getString(R.string.delegation_register_delegation_reduce_warning_content))
        builder.setPositiveButton(getString(R.string.delegation_register_delegation_reduce_warning_ok)) { _, _ -> continueToConfirmation() }
        builder.setNegativeButton(getString(R.string.delegation_register_delegation_reduce_warning_cancel)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun show95PercentWarning() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_more_than_95_title)
        builder.setMessage(getString(R.string.delegation_more_than_95_message))
        builder.setPositiveButton(getString(R.string.delegation_more_than_95_continue)) { _, _ -> continueToConfirmation() }
        builder.setNegativeButton(getString(R.string.delegation_more_than_95_new_stake)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun continueToConfirmation() {
        viewModel.delegationData.amount = CurrencyUtil.toGTUValue(amount.text.toString())
        val intent = if (viewModel.delegationData.amount ?: 0 == 0L)
            Intent(this, DelegationRemoveActivity::class.java)
        else
            Intent(this, DelegationRegisterConfirmationActivity::class.java)
        intent.putExtra(EXTRA_DELEGATION_DATA, viewModel.delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
