package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_DELEGATION
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerRegisterAmountActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.common.StakeAmountInputValidator
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.*
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.amount
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.amount_desc
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.amount_error
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.amount_locked
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.balance_amount
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.pool_estimated_transaction_fee
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.pool_registration_continue

class DelegationRegisterAmountActivity :
    BaseDelegationBakerRegisterAmountActivity(R.layout.activity_delegation_registration_amount, R.string.delegation_register_delegation_title) {

    override fun showError(stakeError: StakeAmountInputValidator.StakeError?) {
        amount.setTextColor(getColor(R.color.text_pink))
        amount_error.visibility = View.VISIBLE
        if (stakeError == StakeAmountInputValidator.StakeError.POOL_LIMIT_REACHED || stakeError == StakeAmountInputValidator.StakeError.POOL_LIMIT_REACHED_COOLDOWN) {
            pool_limit_title.setTextColor(getColor(R.color.text_pink))
            pool_limit.setTextColor(getColor(R.color.text_pink))
        } else {
            pool_limit_title.setTextColor(getColor(R.color.text_black))
            pool_limit.setTextColor(getColor(R.color.text_black))
        }
        if (stakeError == StakeAmountInputValidator.StakeError.POOL_LIMIT_REACHED_COOLDOWN) {
            delegation_amount_title.setTextColor(getColor(R.color.text_pink))
            delegation_amount.setTextColor(getColor(R.color.text_pink))
        }
    }

    override fun hideError() {
        amount.setTextColor(getColor(R.color.theme_blue))
        pool_limit_title.setTextColor(getColor(R.color.text_black))
        pool_limit.setTextColor(getColor(R.color.text_black))
        delegation_amount_title.setTextColor(getColor(R.color.text_black))
        delegation_amount.setTextColor(getColor(R.color.text_black))
        amount_error.visibility = View.INVISIBLE
    }

    override fun loadTransactionFee() {
        viewModel.loadTransactionFee(true)
    }

    private fun showConfirmationPage() {
    }

    override fun initViews() {
        super.initViews()

        if (viewModel.isUpdatingDelegation())
            setActionBarTitle(R.string.delegation_update_delegation_title)

        amount.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onContinueClicked()
                    true
                }
                false
            }
        setAmountHint()
        amount.doOnTextChanged { _, _, _, _ ->
            validateAmountInput()
            pool_registration_continue.isEnabled = hasChanges()
        }
        amount.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus && amount.text.isEmpty()) amount.hint = ""
            else setAmountHint()
        }

        pool_registration_continue.setOnClickListener {
            onContinueClicked()
        }

        balance_amount.text = CurrencyUtil.formatGTU(viewModel.bakerDelegationData.account?.finalizedBalance ?: 0, true)
        delegation_amount.text = CurrencyUtil.formatGTU(0, true)
        viewModel.bakerDelegationData.account?.let { account ->
            account.accountDelegation?.let { accountDelegation ->
                delegation_amount.text = CurrencyUtil.formatGTU(accountDelegation.stakedAmount, true)
            }
        }

        pool_limit.text =
            viewModel.bakerDelegationData.bakerPoolStatus?.let {
                CurrencyUtil.formatGTU(it.delegatedCapitalCap, true)
            }
        current_pool.text =
            viewModel.bakerDelegationData.bakerPoolStatus?.let {
                CurrencyUtil.formatGTU(it.delegatedCapital, true)
            }

        pool_registration_continue.isEnabled = false
        amount.isEnabled = false
        showWaiting(true)

        viewModel.transactionFeeLiveData.observe(this, object : Observer<Pair<Long?, Int?>> {
            override fun onChanged(response: Pair<Long?, Int?>?) {
                response?.first?.let {
                    validateFee = it
                    showWaiting(false)
                    pool_estimated_transaction_fee.visibility = View.VISIBLE
                    pool_estimated_transaction_fee.text = getString(
                        R.string.delegation_register_delegation_amount_estimated_transaction_fee, CurrencyUtil.formatGTU(validateFee ?: 0)
                    )
                    pool_registration_continue.isEnabled = true
                    if (!viewModel.isInCoolDown())
                        amount.isEnabled = true
                }
            }
        })

        loadTransactionFee()

        pool_info.visibility = if (viewModel.bakerDelegationData.isLPool) View.GONE else View.VISIBLE

        updateContent()

        initializeWaitingLiveData()

        viewModel.showDetailedLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showConfirmationPage()
                }
            }
        })

        baseDelegationBakerRegisterAmountListener =
            object : BaseDelegationBakerRegisterAmountListener {
                override fun onRestakeChanged() {
                    pool_registration_continue.isEnabled = hasChanges()
                }
            }
    }

    override fun getStakeAmountInputValidator(): StakeAmountInputValidator {
        return StakeAmountInputValidator(
            if (viewModel.isUpdatingDelegation()) "0" else "1",
            null,
            (viewModel.bakerDelegationData.account?.finalizedBalance ?: 0),
            viewModel.bakerDelegationData.account?.getAtDisposal(),
            viewModel.bakerDelegationData.bakerPoolStatus?.delegatedCapital,
            viewModel.bakerDelegationData.bakerPoolStatus?.delegatedCapitalCap,
            viewModel.bakerDelegationData.account?.accountDelegation?.stakedAmount,
            viewModel.isInCoolDown(),
            viewModel.bakerDelegationData.account?.accountDelegation?.delegationTarget?.bakerId,
            viewModel.bakerDelegationData.poolId)
    }

    override fun errorLiveData(value: Int) {
        showError(null)
    }

    private fun updateContent() {
        if (viewModel.isInCoolDown()) {
            amount_locked.visibility = View.VISIBLE
            amount.isEnabled = false
        }
        if (viewModel.bakerDelegationData.type == UPDATE_DELEGATION) {
            viewModel.bakerDelegationData.oldStakedAmount = viewModel.bakerDelegationData.account?.accountDelegation?.stakedAmount?.toLong() ?: 0
            amount_desc.text = getString(R.string.delegation_update_delegation_amount_enter_amount)
            amount.setText(viewModel.bakerDelegationData.account?.accountDelegation?.stakedAmount?.let { CurrencyUtil.formatGTU(it,false) })
        }
    }

    private fun onContinueClicked() {

        if (!pool_registration_continue.isEnabled) return

        val stakeAmountInputValidator = getStakeAmountInputValidator()
        val stakeError = stakeAmountInputValidator.validate(CurrencyUtil.toGTUValue(amount.text.toString())?.toString(), validateFee)
        if (stakeError != StakeAmountInputValidator.StakeError.OK) {
            amount_error.text = stakeAmountInputValidator.getErrorText(this, stakeError)
            showError(stakeError)
            return
        }

        val amountToStake = getAmountToStake()
        if (viewModel.isUpdatingDelegation()) {
            when {
                !hasChanges() -> showNoChange()
                amountToStake == 0L -> showNewAmountZero()
                amountToStake < (viewModel.bakerDelegationData.account?.accountDelegation?.stakedAmount?.toLongOrNull() ?: 0) -> showReduceWarning()
                moreThan95Percent(amountToStake) -> show95PercentWarning()
                else -> continueToConfirmation()
            }
        } else {
            when {
                moreThan95Percent(amountToStake) -> show95PercentWarning()
                else -> continueToConfirmation()
            }
        }
    }

    private fun hasChanges(): Boolean {
        return !((getAmountToStake() == viewModel.bakerDelegationData.oldStakedAmount &&
                viewModel.getPoolId() == (viewModel.bakerDelegationData.oldDelegationTargetPoolId?.toString() ?: "") &&
                viewModel.bakerDelegationData.restake == viewModel.bakerDelegationData.oldRestake &&
                viewModel.bakerDelegationData.isBakerPool == viewModel.bakerDelegationData.oldDelegationIsBaker))
    }

    private fun getAmountToStake(): Long {
        return CurrencyUtil.toGTUValue(amount.text.toString()) ?: 0
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
        viewModel.bakerDelegationData.amount = CurrencyUtil.toGTUValue(amount.text.toString())
        val intent = if ((viewModel.bakerDelegationData.amount ?: 0) == 0L)
            Intent(this, DelegationRemoveActivity::class.java)
        else
            Intent(this, DelegationRegisterConfirmationActivity::class.java)
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
