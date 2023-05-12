package com.concordium.wallet.ui.bakerdelegation.baker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_BAKER
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityBakerRegistrationAmountBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerRegisterAmountActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.common.StakeAmountInputValidator
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.ui.common.GenericFlowActivity

class BakerRegisterAmountActivity : BaseDelegationBakerRegisterAmountActivity() {
    private var minFee: Long? = null
    private var maxFee: Long? = null
    private var singleFee: Long? = null

    companion object {
        private const val RANGE_MIN_FEE = 1
        private const val RANGE_MAX_FEE = 2
        private const val SINGLE_FEE = 3
    }

    private lateinit var binding: ActivityBakerRegistrationAmountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerRegistrationAmountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.baker_registration_amount_title)
        initViews()
    }

    override fun initViews() {
        super.initViews()
        super.initReStakeOptionsView(binding.restakeOptions)

        if (viewModel.bakerDelegationData.isUpdateBaker()) {
            setActionBarTitle(R.string.baker_registration_update_amount_title)
            viewModel.bakerDelegationData.oldStakedAmount = viewModel.bakerDelegationData.account?.accountBaker?.stakedAmount?.toLong()
            viewModel.bakerDelegationData.oldRestake = viewModel.bakerDelegationData.account?.accountBaker?.restakeEarnings
            binding.amount.setText(CurrencyUtil.formatGTU(viewModel.bakerDelegationData.account?.accountBaker?.stakedAmount ?: "0", true))
            binding.amountDesc.text = getString(R.string.baker_update_enter_new_stake)
        }

        binding.balanceAmount.text = CurrencyUtil.formatGTU(viewModel.bakerDelegationData.account?.finalizedBalance ?: 0, true)
        binding.bakerAmount.text = CurrencyUtil.formatGTU(viewModel.bakerDelegationData.account?.accountBaker?.stakedAmount ?: "0", true)

        viewModel.transactionFeeLiveData.observe(this, object : Observer<Pair<Long?, Int?>> {
            override fun onChanged(response: Pair<Long?, Int?>) {
                response.second?.let { requestId ->
                    when (requestId) {
                        SINGLE_FEE -> {
                            singleFee = response.first
                            validateFee = response.first
                        }
                        RANGE_MIN_FEE -> minFee = response.first
                        RANGE_MAX_FEE -> {
                            maxFee = response.first
                            validateFee = response.first
                        }
                    }
                    singleFee?.let {
                        binding.poolEstimatedTransactionFee.visibility = View.VISIBLE
                        binding.poolEstimatedTransactionFee.text = getString(R.string.baker_registration_update_amount_estimated_transaction_fee_single, CurrencyUtil.formatGTU(singleFee ?: 0))
                    } ?: run {
                        if (minFee != null && maxFee != null) {
                            binding.poolEstimatedTransactionFee.visibility = View.VISIBLE
                            binding.poolEstimatedTransactionFee.text = getString(R.string.baker_registration_update_amount_estimated_transaction_fee_range, CurrencyUtil.formatGTU(minFee ?: 0), CurrencyUtil.formatGTU(maxFee ?: 0))
                        }
                    }
                }
            }
        })

        viewModel.chainParametersPassiveDelegationBakerPoolLoaded.observe(this, Observer { success ->
            success?.let {
                updateViews()
                showWaiting(binding.includeProgress.progressLayout, false)
            }
        })

        showWaiting(binding.includeProgress.progressLayout, true)

        loadTransactionFee()

        try {
            viewModel.loadChainParametersPassiveDelegationAndPossibleBakerPool()
        } catch (ex: Exception) {
            handleBackendError(ex)
        }
    }

    private fun handleBackendError(throwable: Throwable) {
        val stringRes = BackendErrorHandler.getExceptionStringRes(throwable)
        runOnUiThread {
            popup.showSnackbar(binding.root, stringRes)
        }
    }

    private fun updateViews() {
        binding.amount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onContinueClicked()
            }
            false
        }
        setAmountHint(binding.amount)
        binding.amount.doOnTextChanged { _, _, _, _ ->
            validateAmountInput(binding.amount, binding.amountError)
        }
        binding.amount.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.amount.text.isEmpty()) binding.amount.hint = ""
            else setAmountHint(binding.amount)
        }

        binding.bakerRegistrationContinue.setOnClickListener {
            onContinueClicked()
        }

        if (viewModel.isInCoolDown()) {
            binding.amountLocked.visibility = View.VISIBLE
            binding.amount.isEnabled = false
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
            viewModel.bakerDelegationData.account?.getAtDisposal(),
            viewModel.bakerDelegationData.bakerPoolStatus?.delegatedCapital,
            null,
            viewModel.bakerDelegationData.account?.accountDelegation?.stakedAmount,
            viewModel.isInCoolDown(),
            viewModel.bakerDelegationData.account?.accountDelegation?.delegationTarget?.bakerId,
            viewModel.bakerDelegationData.poolId)
    }

    override fun showError(stakeError: StakeAmountInputValidator.StakeError?) {
        binding.amount.setTextColor(getColor(R.color.text_pink))
        binding.amountError.visibility = View.VISIBLE
    }

    override fun hideError() {
        binding.amount.setTextColor(getColor(R.color.theme_blue))
        binding.amountError.visibility = View.INVISIBLE
    }

    override fun errorLiveData(value: Int) {
    }

    private fun onContinueClicked() {
        if (!binding.bakerRegistrationContinue.isEnabled) return

        val stakeAmountInputValidator = getStakeAmountInputValidator()
        val stakeError = stakeAmountInputValidator.validate(CurrencyUtil.toGTUValue(binding.amount.text.toString())?.toString(), validateFee)
        if (stakeError != StakeAmountInputValidator.StakeError.OK) {
            binding.amountError.text = stakeAmountInputValidator.getErrorText(this, stakeError)
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
        return CurrencyUtil.toGTUValue(binding.amount.text.toString()) ?: 0
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
        viewModel.bakerDelegationData.amount = CurrencyUtil.toGTUValue(binding.amount.text.toString())
        val intent = if (viewModel.bakerDelegationData.isUpdateBaker())
            Intent(this, BakerRegistrationConfirmationActivity::class.java)
        else
            Intent(this, BakerRegistrationActivity::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
