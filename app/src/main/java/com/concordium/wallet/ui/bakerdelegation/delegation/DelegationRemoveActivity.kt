package com.concordium.wallet.ui.bakerdelegation.delegation

import android.widget.Toast
import com.concordium.wallet.R
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import kotlinx.android.synthetic.main.activity_delegation_registration_confirmation.*
import kotlinx.android.synthetic.main.activity_delegation_remove.*
import kotlinx.android.synthetic.main.activity_delegation_remove.estimated_transaction_fee
import kotlinx.android.synthetic.main.activity_delegation_remove.submit_delegation_finish
import kotlinx.android.synthetic.main.activity_delegation_remove.submit_delegation_transaction

class DelegationRemoveActivity :
    BaseDelegationActivity(R.layout.activity_delegation_remove, R.string.delegation_remove_delegation_title) {

    override fun initializeViewModel() {
        super.initializeViewModel()
        initializeWaitingLiveData()
    }

    override fun initViews() {
        account_to_remove_delegate_from.text = (viewModel.delegationData.account?.name ?: "").plus("\n\n").plus(viewModel.delegationData.account?.address ?: "")
        estimated_transaction_fee.text = ""

        submit_delegation_transaction.setOnClickListener {
            onContinueClicked()
        }

        submit_delegation_finish.setOnClickListener {
            finishUntilClass(AccountDetailsActivity::class.java.canonicalName)
        }
    }

    private fun onContinueClicked() {
        // viewModel.delegateAmount()
    }

    override fun errorLiveData(value: Int) {
        Toast.makeText(this, getString(value), Toast.LENGTH_SHORT).show()
    }

    override fun showDetailedLiveData(value: Boolean) {
    }

    override fun transactionSuccessLiveData() {
    }

    override fun showWaiting(waiting: Boolean) {
        super.showWaiting(waiting)
        submit_delegation_transaction.isEnabled = !waiting
    }
}
