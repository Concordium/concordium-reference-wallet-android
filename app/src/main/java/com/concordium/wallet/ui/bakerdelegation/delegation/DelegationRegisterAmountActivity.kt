package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.DelegationData
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.bakerdelegation.common.StakeAmountInputValidator
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.view.SegmentedControlView
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.*
import kotlinx.android.synthetic.main.progress.*
import java.text.DecimalFormatSymbols

class DelegationRegisterAmountActivity() :
    BaseActivity(R.layout.activity_delegation_registration_amount, R.string.delegation_register_delegation_title) {

    private lateinit var viewModel: DelegationViewModel

    companion object {
        const val EXTRA_DELEGATION_DATA = "EXTRA_DELEGATION_DATA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize(intent.extras?.getSerializable(EXTRA_DELEGATION_DATA) as DelegationData)
        initViews()
    }

    fun initializeViewModel() {
        showWaiting(false)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(DelegationViewModel::class.java)

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })

        viewModel.showDetailedLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showConfirmationPage()
                }
            }
        })

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(null)
            }
        })
    }

    private fun showError(stakeError: StakeAmountInputValidator.StakeError?) {
        amount.setTextColor(getColor(R.color.text_pink))
        amount_error.visibility = View.VISIBLE
        if (stakeError == StakeAmountInputValidator.StakeError.POOL_LIMIT_REACHED) {
            pool_limit_title.setTextColor(getColor(R.color.text_pink))
            pool_limit.setTextColor(getColor(R.color.text_pink))
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

    fun initViews() {

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
        amount.doOnTextChanged { text, start, count, after ->
            if (text != null && (text.toString().equals(DecimalFormatSymbols.getInstance().decimalSeparator.toString()) || text.filter { it == DecimalFormatSymbols.getInstance().decimalSeparator }.length > 1)) {
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
                viewModel.atDisposal().toString(),
                viewModel.delegationData.bakerPoolStatus?.delegatedCapital,
                viewModel.delegationData.bakerPoolStatus?.delegatedCapitalCap,
                viewModel.delegationData.account?.accountDelegation?.stakedAmount)
            val stakeError = stakeAmountInputValidator.validate(CurrencyUtil.toGTUValue(amount.text.toString())?.toString())
            if (stakeError != StakeAmountInputValidator.StakeError.OK) {
                amount_error.text = stakeAmountInputValidator.getErrorText(this, stakeError)
                showError(stakeError)
            } else {
                hideError()
            }
        }
        amount.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus && amount.text.isEmpty()) amount.hint = ""
            else setAmountHint()
        }

        pool_registration_continue.setOnClickListener {
            onContinueClicked()
        }

        balance_amount.text = CurrencyUtil.formatGTU(viewModel.atDisposal(), true)
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

        pool_info.visibility = if (viewModel.isLPool()) View.GONE else View.VISIBLE
        account_balance.text = if (viewModel.isUpdating()) getString(R.string.delegation_register_delegation_amount_at_disposal) else getString(R.string.delegation_register_delegation_amount_balance)

        viewModel.loadTransactionFee()

        updateContent()
    }

    private fun setAmountHint() {
        if (amount.text.isNotEmpty()) amount.hint = ""
        else amount.hint = "Ͼ0" + DecimalFormatSymbols.getInstance().decimalSeparator + "00"
    }

    private fun updateContent() {
        if(viewModel.delegationData.type == DelegationData.TYPE_UPDATE_DELEGATION){
            amount_desc.setText(getString(R.string.delegation_update_delegation_amount_enter_amount))
            amount.setText(viewModel.delegationData.account?.accountDelegation?.stakedAmount?.let { CurrencyUtil.formatGTU(it,false) })
        }
    }

    private fun onContinueClicked() {

        val stakeError = StakeAmountInputValidator(
            if (viewModel.isUpdating()) "0" else "1",
            null,
            viewModel.atDisposal().toString(),
            viewModel.delegationData.bakerPoolStatus?.delegatedCapital,
            viewModel.delegationData.bakerPoolStatus?.delegatedCapitalCap,
            viewModel.delegationData.account?.accountDelegation?.stakedAmount)
            .validate(CurrencyUtil.toGTUValue(amount.text.toString())?.toString())

        if (stakeError != StakeAmountInputValidator.StakeError.OK) {
            showError(stakeError)
            return
        }

        val amountToStake = CurrencyUtil.toGTUValue(amount.text.toString()) ?: 0
        if (viewModel.isUpdating()) {
            when {
                (amountToStake == viewModel.delegationData.account?.accountDelegation?.stakedAmount?.toLongOrNull() ?: 0 && viewModel.getPoolId() == viewModel.getOldPoolId()) -> showNoChange()
                amountToStake == 0L -> showNewAmountZero()
                amountToStake < viewModel.delegationData.account?.accountDelegation?.stakedAmount?.toLongOrNull() ?: 0 -> showReduceWarning()
                amountToStake > viewModel.atDisposal() * 0.95 -> show95PercentWarning()
                else -> continueToConfirmation()
            }
        } else {
            continueToConfirmation()
        }
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
        val intent = Intent(this, DelegationRegisterConfirmationActivity::class.java)
        viewModel.delegationData.amount = CurrencyUtil.toGTUValue(amount.text.toString())
        intent.putExtra(DelegationRegisterConfirmationActivity.EXTRA_DELEGATION_DATA, viewModel.delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
            pool_registration_continue.isEnabled = false
        } else {
            progress_layout.visibility = View.GONE
            pool_registration_continue.isEnabled = true
        }
    }
}
