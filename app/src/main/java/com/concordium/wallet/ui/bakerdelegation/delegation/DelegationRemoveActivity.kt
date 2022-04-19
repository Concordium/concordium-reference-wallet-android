package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.view.View
import android.widget.Toast
import com.concordium.wallet.R
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.util.UnitConvertUtil
import kotlinx.android.synthetic.main.activity_delegation_remove.*
import kotlinx.android.synthetic.main.activity_delegation_remove.estimated_transaction_fee
import kotlinx.android.synthetic.main.activity_delegation_remove.submit_delegation_finish
import kotlinx.android.synthetic.main.activity_delegation_remove.submit_delegation_transaction
import kotlinx.android.synthetic.main.transaction_submitted_header.*
import kotlinx.android.synthetic.main.transaction_submitted_no.*

class DelegationRemoveActivity :
    BaseDelegationActivity(R.layout.activity_delegation_remove, R.string.delegation_remove_delegation_title) {

    override fun initViews() {
        account_to_remove_delegate_from.text = (viewModel.delegationData.account?.name ?: "").plus("\n\n").plus(viewModel.delegationData.account?.address ?: "")
        estimated_transaction_fee.text = ""

        submit_delegation_transaction.setOnClickListener {
            onContinueClicked()
        }

        submit_delegation_finish.setOnClickListener {
            showNotice()
        }

        initializeWaitingLiveData()
        initializeTransactionFeeLiveData()
        initializeShowAuthenticationLiveData()
        initializeTransactionLiveData()
    }

    private fun onContinueClicked() {
        validate()
    }

    private fun validate() {
        if (viewModel.atDisposal() < viewModel.delegationData.cost ?: 0) {
            showNotEnoughFunds()
        } else {
            if (viewModel.delegationData.isBakerPool) {
                viewModel.delegationData.account?.accountDelegation?.delegationTarget?.bakerId?.let {
                    viewModel.setPoolID(it.toString())
                }
            }
            viewModel.delegationData.amount = 0
            viewModel.delegateAmount()
        }
    }

    private fun showNotEnoughFunds() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_remove_not_enough_funds_title)
        builder.setMessage(getString(R.string.delegation_remove_not_enough_funds_message))
        builder.setPositiveButton(getString(R.string.delegation_remove_not_enough_funds_ok)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    override fun transactionSuccessLiveData() {
        showPageAsReceipt()
    }

    override fun errorLiveData(value: Int) {
        Toast.makeText(this, getString(value), Toast.LENGTH_SHORT).show()
    }

    override fun showDetailedLiveData(value: Boolean) {
    }

    private fun showPageAsReceipt() {
        delegation_remove_text.visibility = View.GONE
        submit_delegation_transaction.visibility = View.GONE
        submit_delegation_finish.visibility = View.VISIBLE
        transaction_submitted.visibility = View.VISIBLE
        viewModel.delegationData.account?.submissionId?.let {
            transaction_submitted_divider.visibility = View.VISIBLE
            transaction_submitted_id.visibility = View.VISIBLE
            transaction_submitted_id.text = it
        }
    }

    private fun showNotice() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_notice_title)
        val gracePeriod = UnitConvertUtil.secondsToDaysRoundedUp(viewModel.delegationData.chainParameters?.delegatorCooldown ?: 0)
        builder.setMessage(resources.getQuantityString(R.plurals.delegation_notice_message_remove, gracePeriod, gracePeriod))
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
