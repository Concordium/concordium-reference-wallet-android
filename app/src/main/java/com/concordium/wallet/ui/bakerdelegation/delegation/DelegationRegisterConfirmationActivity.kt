package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_DELEGATION
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REMOVE_DELEGATION
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_DELEGATION
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityDelegationRegistrationConfirmationBinding
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.util.UnitConvertUtil
import java.math.BigInteger

class DelegationRegisterConfirmationActivity : BaseDelegationBakerActivity() {
    private var receiptMode = false
    private lateinit var binding: ActivityDelegationRegistrationConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDelegationRegistrationConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.delegation_register_delegation_title)
        initViews()
    }

    override fun onBackPressed() {
        if (!receiptMode)
            super.onBackPressed()
    }

    override fun initViews() {
        super.initViews()
        viewModel.chainParametersLoadedLiveData.observe(this, Observer { success ->
            success?.let {
                updateViews()
                showWaiting(binding.includeProgress.progressLayout, false)
            }
        })
        showWaiting(binding.includeProgress.progressLayout, true)
        viewModel.loadChainParameters()
    }

    private fun updateViews() {
        val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(viewModel.bakerDelegationData.chainParameters?.delegatorCooldown ?: 0)

        if (viewModel.isUpdatingDelegation()) {
            setActionBarTitle(R.string.delegation_update_delegation_title)
            binding.delegationTransactionTitle.text = getString(R.string.delegation_update_delegation_transaction_title)
        }

        if (viewModel.bakerDelegationData.type == REGISTER_DELEGATION) {
            binding.gracePeriod.text = resources.getQuantityString(R.plurals.delegation_register_delegation_confirmation_desc, gracePeriod, gracePeriod)
        } else if (viewModel.bakerDelegationData.type == UPDATE_DELEGATION) {
            if (viewModel.isLoweringDelegation()) {
                binding.gracePeriod.text = resources.getQuantityString(R.plurals.delegation_register_delegation_confirmation_desc_update, gracePeriod, gracePeriod)
            }
        }

        binding.submitDelegationTransaction.setOnClickListener {
            onContinueClicked()
        }

        binding.submitDelegationFinish.setOnClickListener {
            showNotice()
        }

        binding.accountToDelegateFrom.text = (viewModel.bakerDelegationData.account?.name ?: "").plus("\n\n").plus(viewModel.bakerDelegationData.account?.address ?: "")
        binding.delegationAmountConfirmation.text = CurrencyUtil.formatGTU(viewModel.bakerDelegationData.amount ?: BigInteger.ZERO, true)
        binding.targetPool.text = if (viewModel.bakerDelegationData.isLPool) getString(R.string.delegation_register_delegation_passive_long) else viewModel.bakerDelegationData.poolId
        binding.rewardsWillBe.text = if (viewModel.bakerDelegationData.restake) getString(R.string.delegation_status_added_to_delegation_amount) else getString(R.string.delegation_status_at_disposal)

        if (!viewModel.stakedAmountHasChanged()) {
            binding.delegationAmountConfirmationTitle.visibility = View.GONE
            binding.delegationAmountConfirmation.visibility = View.GONE
        }
        if (!viewModel.poolHasChanged() && viewModel.isUpdatingDelegation()) {
            binding.targetPoolTitle.visibility = View.GONE
            binding.targetPool.visibility = View.GONE
        }
        if (!viewModel.restakeHasChanged()) {
            binding.rewardsWillBeTitle.visibility = View.GONE
            binding.rewardsWillBe.visibility = View.GONE
        }

        initializeTransactionFeeLiveData(binding.includeProgress.progressLayout, binding.estimatedTransactionFee)
        initializeShowAuthenticationLiveData()
        initializeWaitingLiveData(binding.includeProgress.progressLayout)

        viewModel.transactionSuccessLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showPageAsReceipt()
            }
        })

        viewModel.loadTransactionFee(true)
    }

    override fun errorLiveData(value: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_register_delegation_failed_title)
        val messageFromWalletProxy = getString(value)
        builder.setMessage(getString(R.string.delegation_register_delegation_failed_message, messageFromWalletProxy))
        builder.setPositiveButton(getString(R.string.delegation_register_delegation_failed_try_again)) { dialog, _ ->
            dialog.dismiss()
            onContinueClicked()
        }
        builder.setNegativeButton(getString(R.string.delegation_register_delegation_failed_later)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun showPageAsReceipt() {
        receiptMode = true
        hideActionBarBack()
        binding.submitDelegationTransaction.visibility = View.GONE
        binding.gracePeriod.visibility = View.GONE
        binding.submitDelegationFinish.visibility = View.VISIBLE
        binding.includeTransactionSubmittedHeader.transactionSubmitted.visibility = View.VISIBLE
        viewModel.bakerDelegationData.submissionId?.let {
            binding.includeTransactionSubmittedNo.transactionSubmittedDivider.visibility = View.VISIBLE
            binding.includeTransactionSubmittedNo.transactionSubmittedId.visibility = View.VISIBLE
            binding.includeTransactionSubmittedNo.transactionSubmittedId.text = it
        }
    }

    private fun onContinueClicked() {
        viewModel.prepareTransaction()
    }

    private fun showNotice() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_notice_title)

        if (viewModel.isInCoolDown()) {
            builder.setMessage(getString(R.string.delegation_notice_message_locked))
        } else {
            var gracePeriod = 0
            viewModel.bakerDelegationData.chainParameters?.delegatorCooldown?.let { delegatorCooldown ->
                gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(delegatorCooldown)
            }
            when (viewModel.bakerDelegationData.type) {
                UPDATE_DELEGATION -> {
                    if ((viewModel.bakerDelegationData.amount ?: BigInteger.ZERO) < (viewModel.bakerDelegationData.oldStakedAmount ?: BigInteger.ZERO)) {
                        builder.setMessage(resources.getQuantityString(R.plurals.delegation_notice_message_decrease, gracePeriod, gracePeriod))
                    } else {
                        builder.setMessage(getString(R.string.delegation_notice_message))
                    }
                }
                REMOVE_DELEGATION -> {
                    builder.setMessage(resources.getQuantityString(R.plurals.delegation_notice_message_remove, gracePeriod, gracePeriod))
                }
                else -> {
                    builder.setMessage(getString(R.string.delegation_notice_message))
                }
            }
        }

        builder.setPositiveButton(getString(R.string.delegation_notice_ok)) { dialog, _ ->
            dialog.dismiss()
            finishUntilClass(AccountDetailsActivity::class.java.canonicalName)
        }
        builder.create().show()
    }

    override fun showWaiting(progressLayout: View, waiting: Boolean) {
        super.showWaiting(progressLayout, waiting)
        binding.submitDelegationTransaction.isEnabled = !waiting
    }
}
