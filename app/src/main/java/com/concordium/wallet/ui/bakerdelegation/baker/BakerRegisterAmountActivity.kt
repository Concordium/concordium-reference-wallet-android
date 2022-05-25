package com.concordium.wallet.ui.bakerdelegation.baker

import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_BAKER
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerRegisterAmountActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.common.StakeAmountInputValidator
import com.concordium.wallet.ui.common.GenericFlowActivity
import kotlinx.android.synthetic.main.activity_baker_registration_amount.*
import kotlinx.android.synthetic.main.activity_baker_registration_amount.amount
import kotlinx.android.synthetic.main.activity_baker_registration_amount.amount_desc
import kotlinx.android.synthetic.main.activity_baker_registration_amount.amount_error
import kotlinx.android.synthetic.main.activity_baker_registration_amount.amount_locked
import kotlinx.android.synthetic.main.activity_baker_registration_amount.balance_amount
import kotlinx.android.synthetic.main.activity_baker_registration_amount.pool_estimated_transaction_fee

class BakerRegisterAmountActivity :
    BaseDelegationBakerRegisterAmountActivity(R.layout.activity_baker_registration_amount, R.string.baker_registration_amount_title) {

    private var minFee: Long? = null
    private var maxFee: Long? = null
    private var singleFee: Long? = null

    companion object {
        private const val RANGE_MIN_FEE = 1
        private const val RANGE_MAX_FEE = 2
        private const val SINGLE_FEE = 3
    }

    override fun initViews() {
        super.initViews()

        if (viewModel.bakerDelegationData.isUpdateBaker()) {
            setActionBarTitle(R.string.baker_registration_update_amount_title)
            viewModel.bakerDelegationData.oldStakedAmount = viewModel.bakerDelegationData.account?.accountBaker?.stakedAmount?.toLong()
            viewModel.bakerDelegationData.oldRestake = viewModel.bakerDelegationData.account?.accountBaker?.restakeEarnings
            amount.setText(CurrencyUtil.formatGTU(viewModel.bakerDelegationData.account?.accountBaker?.stakedAmount ?: "0", true))
            amount_desc.text = getString(R.string.baker_update_enter_new_stake)
        }

        balance_amount.text = CurrencyUtil.formatGTU(viewModel.bakerDelegationData.account?.finalizedBalance ?: 0, true)
        baker_amount.text = CurrencyUtil.formatGTU(viewModel.bakerDelegationData.account?.accountBaker?.stakedAmount ?: "0", true)

        viewModel.transactionFeeLiveData.observe(this, object : Observer<Pair<Long?, Int?>> {
            override fun onChanged(response: Pair<Long?, Int?>?) {
                response?.second?.let { requestId ->
                    when (requestId) {
                        SINGLE_FEE -> singleFee = response.first
                        RANGE_MIN_FEE -> minFee = response.first
                        RANGE_MAX_FEE -> maxFee = response.first
                    }
                    singleFee?.let {
                        pool_estimated_transaction_fee.visibility = View.VISIBLE
                        pool_estimated_transaction_fee.text = getString(R.string.baker_registration_update_amount_estimated_transaction_fee_single, CurrencyUtil.formatGTU(it))
                    } ?: run {
                        if (minFee != null && maxFee != null) {
                            pool_estimated_transaction_fee.visibility = View.VISIBLE
                            pool_estimated_transaction_fee.text = getString(R.string.baker_registration_update_amount_estimated_transaction_fee_range, CurrencyUtil.formatGTU(minFee ?: 0), CurrencyUtil.formatGTU(maxFee ?: 0))
                        }
                    }
                }
            }
        })

        viewModel.chainParametersPassiveDelegationBakerPoolLoaded.observe(this, Observer { success ->
            success?.let {
                updateViews()
                showWaiting(false)
            }
        })

        showWaiting(true)
        loadTransactionFee()
        viewModel.loadChainParametersPassiveDelegationAndPossibleBakerPool()
    }

    private fun updateViews() {
        amount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onContinueClicked()
            }
            false
        }
        setAmountHint()
        amount.doOnTextChanged { _, _, _, _ ->
            validateAmountInput()
        }
        amount.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus && amount.text.isEmpty()) amount.hint = ""
            else setAmountHint()
        }

        baker_registration_continue.setOnClickListener {
            onContinueClicked()
        }

        if (viewModel.isInCoolDown()) {
            amount_locked.visibility = View.VISIBLE
            amount.isEnabled = false
        }
    }

    override fun loadTransactionFee() {
        when (viewModel.bakerDelegationData.type) {
            REGISTER_BAKER -> {
                viewModel.loadTransactionFee(true, requestId = RANGE_MIN_FEE, metadataSizeForced = 0)
                viewModel.loadTransactionFee(true, requestId = RANGE_MAX_FEE, metadataSizeForced = 2048)
            }
            else -> viewModel.loadTransactionFee(true, requestId = SINGLE_FEE, metadataSizeForced = viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.metadataUrl?.length)
        }
    }

    override fun getStakeAmountInputValidator(): StakeAmountInputValidator {
        return StakeAmountInputValidator(
            viewModel.bakerDelegationData.chainParameters?.minimumEquityCapital,
            viewModel.getStakeInputMax(),
            (viewModel.bakerDelegationData.account?.finalizedBalance ?: 0),
            viewModel.bakerDelegationData.account?.getAtDisosal(),
            viewModel.bakerDelegationData.bakerPoolStatus?.delegatedCapital,
            null,
            viewModel.bakerDelegationData.account?.accountDelegation?.stakedAmount,
            viewModel.isInCoolDown(),
            viewModel.bakerDelegationData.account?.accountDelegation?.delegationTarget?.bakerId,
            viewModel.bakerDelegationData.poolId)
    }

    override fun showError(stakeError: StakeAmountInputValidator.StakeError?) {
        amount.setTextColor(getColor(R.color.text_pink))
        amount_error.visibility = View.VISIBLE
    }

    override fun hideError() {
        amount.setTextColor(getColor(R.color.theme_blue))
        amount_error.visibility = View.INVISIBLE
    }

    override fun errorLiveData(value: Int) {
    }

    private fun onContinueClicked() {
        if (!baker_registration_continue.isEnabled) return

        val stakeAmountInputValidator = getStakeAmountInputValidator()
        val stakeError = stakeAmountInputValidator.validate(CurrencyUtil.toGTUValue(amount.text.toString())?.toString(), fee)
        if (stakeError != StakeAmountInputValidator.StakeError.OK) {
            amount_error.text = stakeAmountInputValidator.getErrorText(this, stakeError)
            showError(stakeError)
            return
        }

        val amountToStake = getAmountToStake()
        if (viewModel.bakerDelegationData.isUpdateBaker()) {
            when {
                amountToStake == viewModel.bakerDelegationData.oldStakedAmount && viewModel.bakerDelegationData.restake == viewModel.bakerDelegationData.oldRestake -> showNoChange()
                moreThan95Percent(amountToStake) -> show95PercentWarning()
                else -> gotoNextPage()
            }
        } else {
            when {
                moreThan95Percent(amountToStake) -> show95PercentWarning()
                else -> gotoNextPage()
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
        builder.setPositiveButton(getString(R.string.baker_more_than_95_continue)) { _, _ -> gotoNextPage() }
        builder.setNegativeButton(getString(R.string.baker_more_than_95_new_stake)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun gotoNextPage() {
        viewModel.bakerDelegationData.amount = CurrencyUtil.toGTUValue(amount.text.toString())
        val intent = if (viewModel.bakerDelegationData.isUpdateBaker())
            Intent(this, BakerRegistrationConfirmationActivity::class.java)
        else
            Intent(this, BakerRegistrationActivity::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
