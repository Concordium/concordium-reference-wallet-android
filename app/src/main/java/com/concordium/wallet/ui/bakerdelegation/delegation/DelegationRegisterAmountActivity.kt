package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_DELEGATION
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityDelegationRegistrationAmountBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerRegisterAmountActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.common.StakeAmountInputValidator
import com.concordium.wallet.util.toBigInteger
import java.math.BigInteger

class DelegationRegisterAmountActivity : BaseDelegationBakerRegisterAmountActivity() {
    private lateinit var binding: ActivityDelegationRegistrationAmountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDelegationRegistrationAmountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.delegation_register_delegation_title
        )
        initViews()
    }

    override fun showError(stakeError: StakeAmountInputValidator.StakeError?) {
        binding.amount.setTextColor(getColor(R.color.text_pink))
        binding.amountError.visibility = View.VISIBLE
        if (stakeError == StakeAmountInputValidator.StakeError.POOL_LIMIT_REACHED || stakeError == StakeAmountInputValidator.StakeError.POOL_LIMIT_REACHED_COOLDOWN) {
            binding.poolLimitTitle.setTextColor(getColor(R.color.text_pink))
            binding.poolLimit.setTextColor(getColor(R.color.text_pink))
        } else {
            binding.poolLimitTitle.setTextColor(getColor(R.color.text_black))
            binding.poolLimit.setTextColor(getColor(R.color.text_black))
        }
        if (stakeError == StakeAmountInputValidator.StakeError.POOL_LIMIT_REACHED_COOLDOWN) {
            binding.delegationAmountTitle.setTextColor(getColor(R.color.text_pink))
            binding.delegationAmount.setTextColor(getColor(R.color.text_pink))
        }
    }

    override fun hideError() {
        binding.amount.setTextColor(getColor(R.color.theme_blue))
        binding.poolLimitTitle.setTextColor(getColor(R.color.text_black))
        binding.poolLimit.setTextColor(getColor(R.color.text_black))
        binding.delegationAmountTitle.setTextColor(getColor(R.color.text_black))
        binding.delegationAmount.setTextColor(getColor(R.color.text_black))
        binding.amountError.visibility = View.INVISIBLE
    }

    override fun loadTransactionFee() {
        viewModel.loadTransactionFee(true)
    }

    private fun showConfirmationPage() {
    }

    override fun initViews() {
        super.initViews()
        super.initReStakeOptionsView(binding.restakeOptions)

        if (viewModel.isUpdatingDelegation())
            setActionBarTitle(R.string.delegation_update_delegation_title)

        binding.amount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onContinueClicked()
                true
            }
            false
        }
        setAmountHint(binding.amount)
        binding.amount.doOnTextChanged { _, _, _, _ ->
            validateAmountInput(binding.amount, binding.amountError)
            binding.poolRegistrationContinue.isEnabled = hasChanges()
        }
        binding.amount.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.amount.text.isEmpty()) binding.amount.hint = ""
            else setAmountHint(binding.amount)
        }

        binding.poolRegistrationContinue.setOnClickListener {
            onContinueClicked()
        }

        binding.balanceAmount.text = CurrencyUtil.formatGTU(
            viewModel.bakerDelegationData.account?.getAtDisposalWithoutStakedOrScheduled(
                viewModel.bakerDelegationData.account?.totalUnshieldedBalance ?: BigInteger.ZERO
            ) ?: BigInteger.ZERO, true
        )
        binding.delegationAmount.text = CurrencyUtil.formatGTU(BigInteger.ZERO, true)
        viewModel.bakerDelegationData.account?.let { account ->
            account.accountDelegation?.let { accountDelegation ->
                binding.delegationAmount.text =
                    CurrencyUtil.formatGTU(accountDelegation.stakedAmount, true)
            }
        }

        binding.poolLimit.text =
            viewModel.bakerDelegationData.bakerPoolStatus?.let {
                CurrencyUtil.formatGTU(it.delegatedCapitalCap, true)
            }
        binding.currentPool.text =
            viewModel.bakerDelegationData.bakerPoolStatus?.let {
                CurrencyUtil.formatGTU(it.delegatedCapital, true)
            }

        binding.poolRegistrationContinue.isEnabled = false
        binding.amount.isEnabled = false
        showWaiting(binding.includeProgress.progressLayout, true)

        viewModel.transactionFeeLiveData.observe(this) { response ->
            response?.first?.let {
                validateFee = it
                showWaiting(binding.includeProgress.progressLayout, false)
                binding.poolEstimatedTransactionFee.visibility = View.VISIBLE
                binding.poolEstimatedTransactionFee.text = getString(
                    R.string.delegation_register_delegation_amount_estimated_transaction_fee,
                    CurrencyUtil.formatGTU(validateFee ?: BigInteger.ZERO)
                )
                binding.poolRegistrationContinue.isEnabled = true
                if (!viewModel.isInCoolDown())
                    binding.amount.isEnabled = true
            }
        }

        loadTransactionFee()

        binding.poolInfo.visibility =
            if (viewModel.bakerDelegationData.isLPool) View.GONE else View.VISIBLE

        updateContent()

        initializeWaitingLiveData(binding.includeProgress.progressLayout)

        viewModel.showDetailedLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showConfirmationPage()
                }
            }
        })

        baseDelegationBakerRegisterAmountListener =
            object : BaseDelegationBakerRegisterAmountListener {
                override fun onReStakeChanged() {
                    binding.poolRegistrationContinue.isEnabled = hasChanges()
                }
            }
    }

    override fun getStakeAmountInputValidator(): StakeAmountInputValidator {
        return StakeAmountInputValidator(
            if (viewModel.isUpdatingDelegation()) "0" else "1",
            null,
            viewModel.bakerDelegationData.account?.finalizedBalance ?: BigInteger.ZERO,
            viewModel.bakerDelegationData.account?.getAtDisposal(),
            viewModel.bakerDelegationData.bakerPoolStatus?.delegatedCapital,
            viewModel.bakerDelegationData.bakerPoolStatus?.delegatedCapitalCap,
            viewModel.bakerDelegationData.account?.accountDelegation?.stakedAmount,
            viewModel.isInCoolDown(),
            viewModel.bakerDelegationData.account?.accountDelegation?.delegationTarget?.bakerId,
            viewModel.bakerDelegationData.poolId
        )
    }

    override fun errorLiveData(value: Int) {
        showError(null)
    }

    private fun updateContent() {
        if (viewModel.isInCoolDown()) {
            binding.amountLocked.visibility = View.VISIBLE
            binding.amount.isEnabled = false
        }
        if (viewModel.bakerDelegationData.type == UPDATE_DELEGATION) {
            viewModel.bakerDelegationData.oldStakedAmount =
                viewModel.bakerDelegationData.account?.accountDelegation?.stakedAmount.toBigInteger()
            binding.amountDesc.text =
                getString(R.string.delegation_update_delegation_amount_enter_amount)
            binding.amount.setText(viewModel.bakerDelegationData.account?.accountDelegation?.stakedAmount?.let {
                CurrencyUtil.formatGTU(
                    it,
                    false
                )
            })
        }
    }

    private fun onContinueClicked() {

        if (!binding.poolRegistrationContinue.isEnabled) return

        val stakeAmountInputValidator = getStakeAmountInputValidator()
        val stakeError = stakeAmountInputValidator.validate(
            CurrencyUtil.toGTUValue(binding.amount.text.toString())?.toString(), validateFee
        )
        if (stakeError != StakeAmountInputValidator.StakeError.OK) {
            binding.amountError.text = stakeAmountInputValidator.getErrorText(this, stakeError)
            showError(stakeError)
            return
        }

        val amountToStake = getAmountToStake()
        if (viewModel.isUpdatingDelegation()) {
            when {
                !hasChanges() -> showNoChange()
                amountToStake.signum() == 0 -> showNewAmountZero()
                amountToStake < (viewModel.bakerDelegationData.account?.accountDelegation?.stakedAmount.toBigInteger()) -> showReduceWarning()
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
                viewModel.getPoolId() == (viewModel.bakerDelegationData.oldDelegationTargetPoolId?.toString()
            ?: "") &&
                viewModel.bakerDelegationData.restake == viewModel.bakerDelegationData.oldRestake &&
                viewModel.bakerDelegationData.isBakerPool == viewModel.bakerDelegationData.oldDelegationIsBaker))
    }

    private fun getAmountToStake(): BigInteger {
        return CurrencyUtil.toGTUValue(binding.amount.text.toString()) ?: BigInteger.ZERO
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
        viewModel.bakerDelegationData.amount =
            CurrencyUtil.toGTUValue(binding.amount.text.toString())
        val intent = if ((viewModel.bakerDelegationData.amount ?: 0) == 0L)
            Intent(this, DelegationRemoveActivity::class.java)
        else
            Intent(this, DelegationRegisterConfirmationActivity::class.java)
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
