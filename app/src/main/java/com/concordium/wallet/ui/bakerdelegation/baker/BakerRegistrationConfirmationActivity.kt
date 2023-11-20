package com.concordium.wallet.ui.bakerdelegation.baker

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REMOVE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_KEYS
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_STAKE
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityBakerRegistrationConfirmationBinding
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.util.UnitConvertUtil
import com.concordium.wallet.util.dropAfterDecimalPlaces
import com.concordium.wallet.util.toBigInteger
import java.math.BigInteger

class BakerRegistrationConfirmationActivity : BaseDelegationBakerActivity() {
    private var receiptMode = false

    private lateinit var binding: ActivityBakerRegistrationConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerRegistrationConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.baker_registration_confirmation_title
        )
        initViews()
    }

    override fun onBackPressed() {
        if (!receiptMode) super.onBackPressed()
    }

    override fun initViews() {
        super.initViews()
        showWaiting(binding.includeProgress.progressLayout, true)
        loadFee()
    }

    private fun loadFee() {
        viewModel.transactionFeeLiveData.observe(this) { response ->
            response?.first?.let {
                showWaiting(binding.includeProgress.progressLayout, false)
                updateViews()
                binding.estimatedTransactionFee.text = getString(
                    R.string.delegation_register_delegation_amount_estimated_transaction_fee,
                    CurrencyUtil.formatGTU(it)
                )
            }
        }
        viewModel.loadTransactionFee(true)
    }

    private fun updateViews() {

        binding.submitBakerTransaction.text =
            getString(R.string.baker_registration_confirmation_submit)
        binding.accountToBakeFrom.text =
            (viewModel.bakerDelegationData.account?.name ?: "").plus("\n\n")
                .plus(viewModel.bakerDelegationData.account?.address ?: "")
        binding.estimatedTransactionFee.visibility = View.VISIBLE

        when (viewModel.bakerDelegationData.type) {
            REGISTER_BAKER -> {
                updateViewsRegisterBaker()
            }

            UPDATE_BAKER_KEYS -> {
                viewModel.bakerDelegationData.amount =
                    viewModel.bakerDelegationData.account?.accountBaker?.stakedAmount.toBigInteger()
                updateViewsUpdateBakerKeys()
            }

            UPDATE_BAKER_POOL -> {
                updateViewsUpdateBakerPool()
            }

            UPDATE_BAKER_STAKE -> {
                updateViewsUpdateBakerStake()
            }

            REMOVE_BAKER -> {
                updateViewsRemoveBaker()
            }
        }

        binding.submitBakerTransaction.setOnClickListener {
            onContinueClicked()
        }

        binding.submitBakerFinish.setOnClickListener {
            showNotice()
        }

        initializeShowAuthenticationLiveData()
        initializeWaitingLiveData(binding.includeProgress.progressLayout)

        viewModel.transactionSuccessLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showPageAsReceipt()
            }
        })
    }

    private fun updateViewsRegisterBaker() {
        setActionBarTitle(R.string.baker_registration_confirmation_title)
        binding.gracePeriod.text = getString(R.string.baker_registration_confirmation_explain)
        binding.delegationTransactionTitle.text =
            getString(R.string.baker_register_confirmation_receipt_title)
        showAmount()
        showRewards()
        showPoolStatus()
        showCommissionRates()
        showMetaUrl()
        showKeys()
    }

    private fun updateViewsUpdateBakerKeys() {
        setActionBarTitle(R.string.baker_registration_confirmation_update_keys_title)
        binding.gracePeriod.visibility = View.GONE
        binding.delegationTransactionTitle.text =
            getString(R.string.baker_registration_confirmation_update_keys_transaction_title)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_update_affected_account)
        showKeys()
        showCommissionRates()
    }

    private fun updateViewsUpdateBakerPool() {
        setActionBarTitle(R.string.baker_registration_confirmation_update_pool_title)
        binding.delegationTransactionTitle.text =
            getString(R.string.baker_registration_confirmation_update_pool_transaction_title)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_update_affected_account)
        showPoolStatus()
        showCommissionRates()
        showMetaUrl()
    }

    private fun updateViewsUpdateBakerStake() {
        setActionBarTitle(R.string.baker_registration_confirmation_update_stake_title)
        if (viewModel.isUpdateDecreaseAmount()) binding.gracePeriod.text =
            getString(R.string.baker_registration_confirmation_update_stake_update_decrease_explain)
        else binding.gracePeriod.visibility = View.GONE
        binding.delegationTransactionTitle.text =
            getString(R.string.baker_registration_confirmation_update_stake_transaction_title)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_update_stake_update)
        showAmount()
        showRewards()
        showCommissionRates()
    }

    private fun updateViewsRemoveBaker() {
        setActionBarTitle(R.string.baker_registration_confirmation_remove_title)
        binding.gracePeriod.text =
            getString(R.string.baker_registration_confirmation_remove_are_you_sure)
        binding.delegationTransactionTitle.text =
            getString(R.string.baker_registration_confirmation_remove_transaction)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_remove_account_to_stop)
    }

    private fun showAmount() {
        if (viewModel.stakedAmountHasChanged()) {
            binding.delegationAmountConfirmationTitle.visibility = View.VISIBLE
            binding.bakerAmountConfirmation.visibility = View.VISIBLE
            binding.bakerAmountConfirmation.text = CurrencyUtil.formatGTU(
                viewModel.bakerDelegationData.amount ?: BigInteger.ZERO, true
            )
        }
    }

    private fun showRewards() {
        if (viewModel.restakeHasChanged()) {
            binding.rewardsWillBeTitle.visibility = View.VISIBLE
            binding.rewardsWillBe.visibility = View.VISIBLE
            binding.rewardsWillBe.text =
                if (viewModel.bakerDelegationData.restake) getString(R.string.baker_register_confirmation_receipt_added_to_delegation_amount) else getString(
                    R.string.baker_register_confirmation_receipt_at_disposal
                )
        }
    }

    private fun showPoolStatus() {
        if (viewModel.openStatusHasChanged()) {
            binding.poolStatusTitle.visibility = View.VISIBLE
            binding.poolStatus.visibility = View.VISIBLE
            binding.poolStatus.text =
                if (viewModel.isOpenBaker()) getString(R.string.baker_register_confirmation_receipt_pool_status_open) else getString(
                    R.string.baker_register_confirmation_receipt_pool_status_closed
                )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showCommissionRates() {
        binding.apply {
            transactionFeeStatus.text = "${
                viewModel.bakerDelegationData.chainParameters?.transactionCommissionRate
                    ?.times(100)?.dropAfterDecimalPlaces(3) ?: 0.0
            } %"
            bakingStatus.text = "${
                viewModel.bakerDelegationData.chainParameters?.bakingCommissionRate
                    ?.times(100)?.dropAfterDecimalPlaces(3) ?: 0.0
            } %"
        }
    }

    private fun showKeys() {
        binding.electionVerifyKeyTitle.visibility = View.VISIBLE
        binding.electionVerifyKey.visibility = View.VISIBLE
        binding.signatureVerifyKeyTitle.visibility = View.VISIBLE
        binding.signatureVerifyKey.visibility = View.VISIBLE
        binding.aggregationVerifyKeyTitle.visibility = View.VISIBLE
        binding.aggregationVerifyKey.visibility = View.VISIBLE
        binding.electionVerifyKey.text = viewModel.bakerDelegationData.bakerKeys?.electionVerifyKey
        binding.signatureVerifyKey.text =
            viewModel.bakerDelegationData.bakerKeys?.signatureVerifyKey
        binding.aggregationVerifyKey.text =
            viewModel.bakerDelegationData.bakerKeys?.aggregationVerifyKey
    }

    private fun showMetaUrl() {
        if (viewModel.metadataUrlHasChanged()) {
            binding.metaDataUrlTitle.visibility = View.VISIBLE
            binding.metaDataUrl.visibility = View.VISIBLE
            if ((viewModel.bakerDelegationData.metadataUrl?.length
                    ?: 0) > 0
            ) binding.metaDataUrl.text = viewModel.bakerDelegationData.metadataUrl
            else binding.metaDataUrl.text =
                getString(R.string.baker_update_pool_settings_url_removed)
        }
    }

    private fun onContinueClicked() {
        if (viewModel.atDisposal() < (viewModel.bakerDelegationData.cost ?: BigInteger.ZERO)) {
            showNotEnoughFunds()
            return
        }
        viewModel.prepareTransaction()
    }

    private fun showPageAsReceipt() {
        receiptMode = true
        hideActionBarBack()
        binding.submitBakerTransaction.visibility = View.GONE
        binding.submitBakerFinish.visibility = View.VISIBLE
        binding.gracePeriod.visibility = View.GONE
        binding.includeTransactionSubmittedHeader.transactionSubmitted.visibility = View.VISIBLE
        viewModel.bakerDelegationData.submissionId?.let {
            binding.includeTransactionSubmittedNo.transactionSubmittedDivider.visibility =
                View.VISIBLE
            binding.includeTransactionSubmittedNo.transactionSubmittedId.visibility = View.VISIBLE
            binding.includeTransactionSubmittedNo.transactionSubmittedId.text = it
        }
    }

    private fun showNotice() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.baker_notice_title)

        var noticeMessage = getString(R.string.baker_notice_message)

        if (viewModel.bakerDelegationData.type == UPDATE_BAKER_STAKE && (viewModel.bakerDelegationData.oldStakedAmount
                ?: BigInteger.ZERO) < (viewModel.bakerDelegationData.amount ?: BigInteger.ZERO)
        ) {
            noticeMessage = getString(R.string.baker_notice_message_update_increase)
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_STAKE && (viewModel.bakerDelegationData.oldStakedAmount
                ?: BigInteger.ZERO) > (viewModel.bakerDelegationData.amount ?: BigInteger.ZERO)
        ) {
            val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(
                viewModel.bakerDelegationData.chainParameters?.delegatorCooldown ?: 0
            )
            noticeMessage = resources.getQuantityString(
                R.plurals.baker_notice_message_update_decrease, gracePeriod, gracePeriod
            )
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_STAKE && (viewModel.bakerDelegationData.oldStakedAmount
                ?: BigInteger.ZERO) == (viewModel.bakerDelegationData.amount ?: BigInteger.ZERO)
        ) {
            noticeMessage = getString(R.string.baker_notice_message_update_pool)
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_POOL) {
            noticeMessage = getString(R.string.baker_notice_message_update_pool)
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_KEYS) {
            noticeMessage = getString(R.string.baker_notice_message_update_keys)
        } else if (viewModel.bakerDelegationData.type == REMOVE_BAKER) {
            val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(
                viewModel.bakerDelegationData.chainParameters?.poolOwnerCooldown ?: 0
            )
            noticeMessage = resources.getQuantityString(
                R.plurals.baker_notice_message_remove, gracePeriod, gracePeriod
            )
        }

        builder.setMessage(noticeMessage)

        builder.setPositiveButton(getString(R.string.baker_notice_ok)) { dialog, _ ->
            dialog.dismiss()
            finishUntilClass(AccountDetailsActivity::class.java.canonicalName)
        }
        builder.create().show()
    }

    override fun errorLiveData(value: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_register_delegation_failed_title)
        val messageFromWalletProxy = getString(value)
        builder.setMessage(
            getString(
                R.string.baker_register_transaction_failed, messageFromWalletProxy
            )
        )
        builder.setPositiveButton(getString(R.string.delegation_register_delegation_failed_try_again)) { dialog, _ ->
            dialog.dismiss()
            onContinueClicked()
        }
        builder.setNegativeButton(getString(R.string.delegation_register_delegation_failed_later)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    override fun showWaiting(progressLayout: View, waiting: Boolean) {
        super.showWaiting(progressLayout, waiting)
        binding.submitBakerTransaction.isEnabled = !waiting
    }
}
