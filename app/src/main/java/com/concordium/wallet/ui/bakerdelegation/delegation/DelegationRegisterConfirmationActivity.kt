package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.util.UnitConvertUtil
import kotlinx.android.synthetic.main.activity_delegation_registration_confirmation.*
import kotlinx.android.synthetic.main.activity_delegation_registration_confirmation.submit_delegation_finish
import kotlinx.android.synthetic.main.activity_delegation_registration_confirmation.submit_delegation_transaction
import kotlinx.android.synthetic.main.transaction_submitted_header.*
import kotlinx.android.synthetic.main.transaction_submitted_no.*

class DelegationRegisterConfirmationActivity :
    BaseDelegationActivity(R.layout.activity_delegation_registration_confirmation, R.string.delegation_register_delegation_title) {

    override fun initViews() {
        val gracePeriod = UnitConvertUtil.secondsToDaysRoundedUp(viewModel.delegationData.chainParameters?.delegatorCooldown ?: 0)

        if (viewModel.isUpdating()) {
            setActionBarTitle(R.string.delegation_update_delegation_title)
            delegation_transaction_title.text = getString(R.string.delegation_update_delegation_transaction_title)
            grace_period.text = resources.getQuantityString(R.plurals.delegation_register_delegation_confirmation_desc_update, gracePeriod, gracePeriod)
        } else {
            grace_period.text = resources.getQuantityString(R.plurals.delegation_register_delegation_confirmation_desc, gracePeriod, gracePeriod)
        }

        submit_delegation_transaction.setOnClickListener {
            onContinueClicked()
        }

        submit_delegation_finish.setOnClickListener {
            showNotice()
        }

        account_to_delegate_from.text = (viewModel.delegationData.account?.name ?: "").plus("\n\n").plus(viewModel.delegationData.account?.address ?: "")
        delegation_amount_confirmation.text = CurrencyUtil.formatGTU(viewModel.delegationData.amount ?: 0, true)
        target_pool.text = if (viewModel.delegationData.isLPool) getString(R.string.delegation_register_delegation_pool_l) else viewModel.delegationData.poolId
        rewards_will_be.text = if (viewModel.delegationData.restake) getString(R.string.delegation_status_added_to_delegation_amount) else getString(R.string.delegation_status_at_disposal)

        initializeTransactionFeeLiveData()
        initializeShowAuthenticationLiveData()
        initializeTransactionLiveData()
        initializeWaitingLiveData()
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

    override fun transactionSuccessLiveData() {
        showPageAsReceipt()
    }

    override fun showDetailedLiveData(value: Boolean) {
    }

    private fun showPageAsReceipt() {
        submit_delegation_transaction.visibility = View.GONE
        grace_period.visibility = View.GONE
        submit_delegation_finish.visibility = View.VISIBLE
        transaction_submitted.visibility = View.VISIBLE
        viewModel.delegationData.account?.submissionId?.let {
            transaction_submitted_divider.visibility = View.VISIBLE
            transaction_submitted_id.visibility = View.VISIBLE
            transaction_submitted_id.text = it
        }
        if (viewModel.isUpdating()) {
            showNotice()
        }
    }

    private fun onContinueClicked() {
        viewModel.delegateAmount()
    }

    private fun showNotice() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_notice_title)

        if (viewModel.isInCoolDown()) {
            builder.setMessage(getString(R.string.delegation_notice_message_locked))
        } else {
            val gracePeriod = UnitConvertUtil.secondsToDaysRoundedUp(viewModel.delegationData.chainParameters?.delegatorCooldown ?: 0)
            if (gracePeriod > 0) {
                builder.setMessage(resources.getQuantityString(R.plurals.delegation_notice_message_decrease, gracePeriod, gracePeriod))
            } else {
                builder.setMessage(getString(R.string.delegation_notice_message))
            }
        }

        builder.setPositiveButton(getString(R.string.delegation_notice_ok)) { dialog, _ ->
            dialog.dismiss()
            finishUntilClass(AccountDetailsActivity::class.java.canonicalName, DelegationStatusActivity::class.java.canonicalName, EXTRA_DELEGATION_DATA, viewModel.delegationData)
        }
        builder.create().show()
    }

    override fun showWaiting(waiting: Boolean) {
        super.showWaiting(waiting)
        submit_delegation_transaction.isEnabled = !waiting
    }
}
