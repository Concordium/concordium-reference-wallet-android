package com.concordium.wallet.ui.bakerdelegation.baker

import android.app.AlertDialog
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import kotlinx.android.synthetic.main.activity_baker_registration_confirmation.*
import kotlinx.android.synthetic.main.activity_baker_registration_confirmation.grace_period
import kotlinx.android.synthetic.main.activity_baker_registration_confirmation.rewards_will_be
import kotlinx.android.synthetic.main.activity_baker_registration_confirmation.rewards_will_be_title
import kotlinx.android.synthetic.main.transaction_submitted_header.*
import kotlinx.android.synthetic.main.transaction_submitted_no.*

class BakerRegistrationConfirmationActivity :
    BaseDelegationBakerActivity(R.layout.activity_baker_registration_confirmation, R.string.baker_registration_confirmation_title) {

    override fun initViews() {
        if (viewModel.isUpdatingBaker()) {
            // setActionBarTitle(R.string.delegation_update_delegation_title)
            //delegation_transaction_title.text = getString(R.string.delegation_update_delegation_transaction_title)
        }

        submit_baker_transaction.setOnClickListener {
            onContinueClicked()
        }

        submit_baker_finish.setOnClickListener {
            showNotice()
        }

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

        initializeTransactionFeeLiveData()
        initializeShowAuthenticationLiveData()
        initializeTransactionLiveData()
        initializeWaitingLiveData()

        viewModel.loadTransactionFee(true)
    }

    private fun onContinueClicked() {
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
        if (viewModel.isUpdatingBaker()) {
            showNotice()
        }
    }

    private fun showNotice() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.baker_notice_title)
        builder.setMessage(getString(R.string.baker_notice_message))
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
