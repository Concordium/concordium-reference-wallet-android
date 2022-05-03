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

        if (viewModel.bakerDelegationData.type == REMOVE_BAKER) {
            setActionBarTitle(R.string.baker_registration_confirmation_remove_title)
            grace_period.text = getString(R.string.baker_registration_confirmation_remove_are_you_sure)
            delegation_transaction_title.text = getString(R.string.baker_registration_confirmation_remove_transaction)
            delegation_transaction_title.setTextColor(getColor(R.color.text_red))
            account_to_bake_title.text = getString(R.string.baker_registration_confirmation_remove_account_to_stop)
            account_to_bake_from.text = (viewModel.bakerDelegationData.account?.name ?: "").plus("\n\n").plus(viewModel.bakerDelegationData.account?.address ?: "")
            delegation_amount_confirmation_title.visibility = View.GONE
            baker_amount_confirmation.visibility = View.GONE
            rewards_will_be_title.visibility = View.GONE
            rewards_will_be.visibility = View.GONE
            pool_status_title.visibility = View.GONE
            pool_status.visibility = View.GONE
            meta_data_url_title.visibility = View.GONE
            meta_data_url.visibility = View.GONE
            election_verify_key_title.visibility = View.GONE
            election_verify_key.visibility = View.GONE
            signature_verify_key_title.visibility = View.GONE
            signature_verify_key.visibility = View.GONE
            aggregation_verify_key_title.visibility = View.GONE
            aggregation_verify_key.visibility = View.GONE
        } else if (viewModel.bakerDelegationData.type == REGISTER_BAKER) {
            account_to_bake_from.text = (viewModel.bakerDelegationData.account?.name ?: "").plus("\n\n").plus(viewModel.bakerDelegationData.account?.address ?: "")
            baker_amount_confirmation.text = CurrencyUtil.formatGTU(viewModel.bakerDelegationData.amount ?: 0, true)
            rewards_will_be.text = if (viewModel.bakerDelegationData.restake) getString(R.string.baker_register_confirmation_receipt_added_to_delegation_amount) else getString(R.string.baker_register_confirmation_receipt_at_disposal)
            pool_status.text = if (viewModel.isOpenBaker()) getString(R.string.baker_register_confirmation_receipt_pool_status_open) else getString(R.string.baker_register_confirmation_receipt_pool_status_closed)

            if (viewModel.isOpenBaker()) {
                meta_data_url_title.visibility = View.VISIBLE
                meta_data_url.visibility = View.VISIBLE
                meta_data_url.text = viewModel.bakerDelegationData.metadataUrl
            }

            election_verify_key.text = viewModel.bakerDelegationData.bakerKeys?.electionVerifyKey
            signature_verify_key.text = viewModel.bakerDelegationData.bakerKeys?.signatureVerifyKey
            aggregation_verify_key.text = viewModel.bakerDelegationData.bakerKeys?.aggregationVerifyKey

            if (!viewModel.stakedAmountHasChanged()) {
                //delegation_amount_confirmation_title.visibility = View.GONE
                //delegation_amount_confirmation.visibility = View.GONE
            }
            if (!viewModel.restakeHasChanged()) {
                rewards_will_be_title.visibility = View.GONE
                rewards_will_be.visibility = View.GONE
            }
        } else {
            // setActionBarTitle(R.string.delegation_update_delegation_title)
            //delegation_transaction_title.text = getString(R.string.delegation_update_delegation_transaction_title)
        }

        submit_baker_transaction.setOnClickListener {
            onContinueClicked()
        }

        submit_baker_finish.setOnClickListener {
            showNotice()
        }

        initializeShowAuthenticationLiveData()
        initializeTransactionLiveData()
        initializeWaitingLiveData()
    }

    private fun onContinueClicked() {
        if (viewModel.atDisposal() < viewModel.bakerDelegationData.cost ?: 0) {
            showNotEnoughFunds()
            return
        }
        viewModel.prepareTransaction()
    }

    override fun transactionSuccessLiveData() {
        showPageAsReceipt()
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

        if (viewModel.bakerDelegationData.type == UPDATE_BAKER_STAKE && viewModel.bakerDelegationData.oldStakedAmount ?: 0 > viewModel.bakerDelegationData.amount ?: 0) {
            noticeMessage = getString(R.string.baker_notice_message_update_increase)
        }  else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_STAKE && viewModel.bakerDelegationData.oldStakedAmount ?: 0 < viewModel.bakerDelegationData.amount ?: 0) {
            val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(viewModel.bakerDelegationData.chainParameters?.delegatorCooldown ?: 0)
            noticeMessage = resources.getQuantityString(R.plurals.baker_notice_message_update_decrease, gracePeriod, gracePeriod)
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_POOL) {
            noticeMessage = getString(R.string.baker_notice_message_update_pool)
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_KEYS) {
            noticeMessage = getString(R.string.baker_notice_message_update_keys)
        } else if (viewModel.bakerDelegationData.type == REMOVE_BAKER) {
            noticeMessage = getString(R.string.baker_notice_message_remove)
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

    override fun showDetailedLiveData(value: Boolean) {
    }

    override fun showWaiting(waiting: Boolean) {
        super.showWaiting(waiting)
        submit_baker_transaction.isEnabled = !waiting
    }
}
