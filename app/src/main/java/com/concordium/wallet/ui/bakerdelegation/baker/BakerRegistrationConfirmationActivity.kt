package com.concordium.wallet.ui.bakerdelegation.baker

import android.app.AlertDialog
import android.view.View
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REMOVE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_KEYS
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_STAKE
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.util.UnitConvertUtil
import kotlinx.android.synthetic.main.activity_baker_registration_confirmation.*
import kotlinx.android.synthetic.main.activity_baker_registration_confirmation.estimated_transaction_fee
import kotlinx.android.synthetic.main.activity_baker_registration_confirmation.grace_period
import kotlinx.android.synthetic.main.activity_baker_registration_confirmation.rewards_will_be
import kotlinx.android.synthetic.main.activity_baker_registration_confirmation.rewards_will_be_title
import kotlinx.android.synthetic.main.transaction_submitted_header.*
import kotlinx.android.synthetic.main.transaction_submitted_no.*

class BakerRegistrationConfirmationActivity :
    BaseDelegationBakerActivity(R.layout.activity_baker_registration_confirmation, R.string.baker_registration_confirmation_title) {

    override fun initViews() {
        super.initViews()
        viewModel.chainParametersLoadedLiveData.observe(this, Observer { success ->
            success?.let {
                loadFee()
            }
        })
        showWaiting(true)
        viewModel.loadChainParameters()
    }

    private fun loadFee() {
        viewModel.transactionFeeLiveData.observe(this, object : Observer<Pair<Long?, Int?>> {
            override fun onChanged(response: Pair<Long?, Int?>?) {
                response?.first?.let {
                    showWaiting(false)
                    updateViews()
                    estimated_transaction_fee.text = getString(R.string.delegation_register_delegation_amount_estimated_transaction_fee, CurrencyUtil.formatGTU(it))
                }
            }
        })
        viewModel.loadTransactionFee(true)
    }

    private fun updateViews() {

        submit_baker_transaction.text = getString(R.string.baker_registration_confirmation_submit)
        account_to_bake_from.text = (viewModel.bakerDelegationData.account?.name ?: "").plus("\n\n").plus(viewModel.bakerDelegationData.account?.address ?: "")
        estimated_transaction_fee.visibility = View.VISIBLE

        when (viewModel.bakerDelegationData.type) {
            REGISTER_BAKER -> {
                updateViewsRegisterBaker()
            }
            UPDATE_BAKER_KEYS -> {
                viewModel.bakerDelegationData.amount = viewModel.bakerDelegationData.account?.accountBaker?.stakedAmount?.toLong() ?: 0
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

        submit_baker_transaction.setOnClickListener {
            onContinueClicked()
        }

        submit_baker_finish.setOnClickListener {
            showNotice()
        }

        initializeShowAuthenticationLiveData()
        initializeWaitingLiveData()

        viewModel.transactionSuccessLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showPageAsReceipt()
            }
        })
    }

    private fun updateViewsRegisterBaker() {
        setActionBarTitle(R.string.baker_registration_confirmation_title)
        grace_period.text = getString(R.string.baker_registration_confirmation_explain)
        delegation_transaction_title.text = getString(R.string.baker_register_confirmation_receipt_title)
        showAmount()
        showRewards()
        showPoolStatus()
        showMetaUrl()
        showKeys()
    }

    private fun updateViewsUpdateBakerKeys() {
        setActionBarTitle(R.string.baker_registration_confirmation_update_keys_title)
        grace_period.visibility = View.GONE
        delegation_transaction_title.text = getString(R.string.baker_registration_confirmation_update_keys_transaction_title)
        account_to_bake_title.text = getString(R.string.baker_registration_confirmation_update_affected_account)
        showKeys()
    }

    private fun updateViewsUpdateBakerPool() {
        setActionBarTitle(R.string.baker_registration_confirmation_update_pool_title)
        delegation_transaction_title.text = getString(R.string.baker_registration_confirmation_update_pool_transaction_title)
        account_to_bake_title.text = getString(R.string.baker_registration_confirmation_update_affected_account)
        showPoolStatus()
        showMetaUrl()
    }

    private fun updateViewsUpdateBakerStake() {
        setActionBarTitle(R.string.baker_registration_confirmation_update_stake_title)
        if (viewModel.isUpdateDecreaseAmount()) grace_period.text = getString(R.string.baker_registration_confirmation_update_stake_update_decrease_explain)
        else grace_period.visibility = View.GONE
        delegation_transaction_title.text = getString(R.string.baker_registration_confirmation_update_stake_transaction_title)
        account_to_bake_title.text = getString(R.string.baker_registration_confirmation_update_stake_update)
        showAmount()
        showRewards()
    }

    private fun updateViewsRemoveBaker() {
        setActionBarTitle(R.string.baker_registration_confirmation_remove_title)
        grace_period.text = getString(R.string.baker_registration_confirmation_remove_are_you_sure)
        delegation_transaction_title.text = getString(R.string.baker_registration_confirmation_remove_transaction)
        account_to_bake_title.text = getString(R.string.baker_registration_confirmation_remove_account_to_stop)
    }

    private fun showAmount() {
        if (viewModel.stakedAmountHasChanged()) {
            delegation_amount_confirmation_title.visibility = View.VISIBLE
            baker_amount_confirmation.visibility = View.VISIBLE
            baker_amount_confirmation.text = CurrencyUtil.formatGTU(viewModel.bakerDelegationData.amount ?: 0, true)
        }
    }

    private fun showRewards() {
        if (viewModel.restakeHasChanged()) {
            rewards_will_be_title.visibility = View.VISIBLE
            rewards_will_be.visibility = View.VISIBLE
            rewards_will_be.text = if (viewModel.bakerDelegationData.restake) getString(R.string.baker_register_confirmation_receipt_added_to_delegation_amount) else getString(R.string.baker_register_confirmation_receipt_at_disposal)
        }
    }

    private fun showPoolStatus() {
        if (viewModel.openStatusHasChanged()) {
            pool_status_title.visibility = View.VISIBLE
            pool_status.visibility = View.VISIBLE
            pool_status.text = if (viewModel.isOpenBaker()) getString(R.string.baker_register_confirmation_receipt_pool_status_open) else getString(R.string.baker_register_confirmation_receipt_pool_status_closed)
        }
    }

    private fun showKeys() {
        election_verify_key_title.visibility = View.VISIBLE
        election_verify_key.visibility = View.VISIBLE
        signature_verify_key_title.visibility = View.VISIBLE
        signature_verify_key.visibility = View.VISIBLE
        aggregation_verify_key_title.visibility = View.VISIBLE
        aggregation_verify_key.visibility = View.VISIBLE
        election_verify_key.text = viewModel.bakerDelegationData.bakerKeys?.electionVerifyKey
        signature_verify_key.text = viewModel.bakerDelegationData.bakerKeys?.signatureVerifyKey
        aggregation_verify_key.text = viewModel.bakerDelegationData.bakerKeys?.aggregationVerifyKey
    }

    private fun showMetaUrl() {
        if (viewModel.metadataUrlHasChanged()) {
            meta_data_url_title.visibility = View.VISIBLE
            meta_data_url.visibility = View.VISIBLE
            if ((viewModel.bakerDelegationData.metadataUrl?.length ?: 0) > 0) meta_data_url.text = viewModel.bakerDelegationData.metadataUrl
            else meta_data_url.text = getString(R.string.baker_update_pool_settings_url_removed)
        }
    }

    private fun onContinueClicked() {
        if (viewModel.atDisposal() < (viewModel.bakerDelegationData.cost ?: 0)) {
            showNotEnoughFunds()
            return
        }
        viewModel.prepareTransaction()
    }

    private fun showPageAsReceipt() {
        submit_baker_transaction.visibility = View.GONE
        submit_baker_finish.visibility = View.VISIBLE
        grace_period.visibility = View.GONE
        transaction_submitted.visibility = View.VISIBLE
        viewModel.bakerDelegationData.submissionId?.let {
            transaction_submitted_divider.visibility = View.VISIBLE
            transaction_submitted_id.visibility = View.VISIBLE
            transaction_submitted_id.text = it
        }
    }

    private fun showNotice() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.baker_notice_title)

        var noticeMessage = getString(R.string.baker_notice_message)

        if (viewModel.bakerDelegationData.type == UPDATE_BAKER_STAKE && (viewModel.bakerDelegationData.oldStakedAmount ?: 0) < (viewModel.bakerDelegationData.amount ?: 0)) {
            noticeMessage = getString(R.string.baker_notice_message_update_increase)
        }  else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_STAKE && (viewModel.bakerDelegationData.oldStakedAmount ?: 0) > (viewModel.bakerDelegationData.amount ?: 0)) {
            val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(viewModel.bakerDelegationData.chainParameters?.delegatorCooldown ?: 0)
            noticeMessage = resources.getQuantityString(R.plurals.baker_notice_message_update_decrease, gracePeriod, gracePeriod)
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_STAKE && (viewModel.bakerDelegationData.oldStakedAmount ?: 0) == (viewModel.bakerDelegationData.amount ?: 0)) {
            noticeMessage = getString(R.string.baker_notice_message_update_pool)
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_POOL) {
            noticeMessage = getString(R.string.baker_notice_message_update_pool)
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_KEYS) {
            noticeMessage = getString(R.string.baker_notice_message_update_keys)
        } else if (viewModel.bakerDelegationData.type == REMOVE_BAKER) {
            val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(viewModel.bakerDelegationData.chainParameters?.delegatorCooldown ?: 0)
            noticeMessage = resources.getQuantityString(R.plurals.baker_notice_message_remove, gracePeriod, gracePeriod)
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
        builder.setMessage(getString(R.string.baker_register_transaction_failed, messageFromWalletProxy))
        builder.setPositiveButton(getString(R.string.delegation_register_delegation_failed_try_again)) { dialog, _ ->
            dialog.dismiss()
            onContinueClicked()
        }
        builder.setNegativeButton(getString(R.string.delegation_register_delegation_failed_later)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    override fun showWaiting(waiting: Boolean) {
        super.showWaiting(waiting)
        submit_baker_transaction.isEnabled = !waiting
    }
}
