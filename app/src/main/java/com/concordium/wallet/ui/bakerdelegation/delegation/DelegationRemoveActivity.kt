package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.view.View
import android.widget.Toast
import com.concordium.wallet.R
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.util.UnitConvertUtil
import kotlinx.android.synthetic.main.activity_delegation_remove.*
import kotlinx.android.synthetic.main.transaction_submitted_header.*
import kotlinx.android.synthetic.main.transaction_submitted_no.*

class DelegationRemoveActivity :
    BaseDelegationBakerActivity(R.layout.activity_delegation_remove, R.string.delegation_remove_delegation_title) {

    override fun initViews() {
        account_to_remove_delegate_from.text = (viewModel.bakerDelegationData.account?.name ?: "").plus("\n\n").plus(viewModel.bakerDelegationData.account?.address ?: "")
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

        viewModel.loadTransactionFee(true)
    }

    private fun onContinueClicked() {
        validate()
    }

    private fun validate() {
        if (viewModel.atDisposal() < viewModel.bakerDelegationData.cost ?: 0) {
            showNotEnoughFunds()
        } else {
            if (viewModel.bakerDelegationData.isBakerPool) {
                viewModel.bakerDelegationData.account?.accountDelegation?.delegationTarget?.bakerId?.let {
                    viewModel.setPoolID(it.toString())
                }
            }
            viewModel.bakerDelegationData.amount = 0
            viewModel.prepareTransaction()
        }
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
        viewModel.bakerDelegationData.submissionId?.let {
            transaction_submitted_divider.visibility = View.VISIBLE
            transaction_submitted_id.visibility = View.VISIBLE
            transaction_submitted_id.text = it
        }
    }

    private fun showNotice() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_notice_title)
        val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(viewModel.bakerDelegationData.chainParameters?.delegatorCooldown ?: 0)
        builder.setMessage(resources.getQuantityString(R.plurals.delegation_notice_message_remove, gracePeriod, gracePeriod))
        builder.setPositiveButton(getString(R.string.delegation_notice_ok)) { dialog, _ ->
            dialog.dismiss()
            finishUntilClass(AccountDetailsActivity::class.java.canonicalName)
        }
        builder.create().show()
    }

    override fun showWaiting(waiting: Boolean) {
        super.showWaiting(waiting)
        submit_delegation_transaction.isEnabled = !waiting
    }
}
