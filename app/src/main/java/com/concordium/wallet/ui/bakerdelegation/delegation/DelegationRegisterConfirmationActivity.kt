package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.util.UnitConvertUtil
import kotlinx.android.synthetic.main.activity_delegation_registration_confirmation.*
import kotlinx.android.synthetic.main.activity_delegation_registration_confirmation.submit_delegation_finish
import kotlinx.android.synthetic.main.activity_delegation_registration_confirmation.submit_delegation_transaction
import kotlinx.android.synthetic.main.transaction_submitted_header.*
import kotlinx.android.synthetic.main.transaction_submitted_no.*

class DelegationRegisterConfirmationActivity :
    BaseDelegationActivity(R.layout.activity_delegation_registration_confirmation, R.string.delegation_register_delegation_title) {

    override fun initializeViewModel() {
        super.initializeViewModel()
        initializeWaitingLiveData()
        initializeTransactionLiveData()
        initializeShowAuthenticationLiveData()
    }

    override fun initViews() {
        if (viewModel.isUpdating())
            setActionBarTitle(R.string.delegation_update_delegation_title)

        submit_delegation_transaction.setOnClickListener {
            onContinueClicked()
        }

        submit_delegation_finish.setOnClickListener {
            finishUntilClass(AccountDetailsActivity::class.java.canonicalName)
        }

        val gracePeriod = UnitConvertUtil.secondsToDaysRoundedUp(viewModel.delegationData.chainParameters?.delegatorCooldown ?: 0)
        grace_period.text = resources.getQuantityString(R.plurals.delegation_register_delegation_confirmation_desc, gracePeriod, gracePeriod)
        account_to_delegate_from.text = (viewModel.delegationData.account?.name ?: "").plus("\n\n").plus(viewModel.delegationData.account?.address ?: "")
        delegation_amount_confirmation.text = CurrencyUtil.formatGTU(viewModel.delegationData.amount ?: 0, true)
        target_pool.text = if (viewModel.isLPool()) getString(R.string.delegation_register_delegation_pool_l) else viewModel.delegationData.poolId
        rewards_will_be.text = if (viewModel.delegationData.restake) getString(R.string.delegation_status_added_to_delegation_amount) else getString(R.string.delegation_status_at_disposal)

        initializeTransactionFeeLiveData()
        initializeShowAuthenticationLiveData()
    }

    override fun errorLiveData(value: Int) {
        Toast.makeText(this, getString(value), Toast.LENGTH_SHORT).show()
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
        transaction_submitted_divider.visibility = View.VISIBLE
        transaction_submitted_transaction_no.visibility = View.VISIBLE
        transaction_submitted_transaction_no.text = viewModel.delegationData.bakerPoolStatus?.bakerAddress ?: ""
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
        builder.setMessage(getString(R.string.delegation_notice_message))
        builder.setPositiveButton(getString(R.string.delegation_notice_ok)) { dialog, _ ->
            dialog.dismiss()
            val intent = Intent(this, DelegationStatusActivity::class.java)
            intent.putExtra(BaseDelegationBakerFlowActivity.EXTRA_DELEGATION_DATA, viewModel.delegationData)
            startActivity(intent)
            finish()
        }
        builder.create().show()
    }

    override fun showWaiting(waiting: Boolean) {
        super.showWaiting(waiting)
        submit_delegation_transaction.isEnabled = !waiting
    }
}
