package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.DelegationData
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.UnitConvertUtil
import kotlinx.android.synthetic.main.activity_delegation_registration_confirmation.*
import kotlinx.android.synthetic.main.progress.*
import javax.crypto.Cipher

class DelegationRegisterConfirmationActivity() :
    BaseActivity(R.layout.activity_delegation_registration_confirmation, R.string.delegation_register_delegation_title) {

    private lateinit var viewModel: DelegationViewModel

    companion object {
        const val EXTRA_DELEGATION_DATA = "EXTRA_DELEGATION_DATA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize(intent.extras?.getSerializable(EXTRA_DELEGATION_DATA) as DelegationData)
        initViews()
    }

    fun initializeViewModel() {
        showWaiting(false)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(DelegationViewModel::class.java)

        viewModel.transactionSuccessLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showPageAsReceipt()
            }
        })

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
    }

    private fun showError(value: Int) {
        Toast.makeText(this, getString(value), Toast.LENGTH_SHORT).show()
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

    fun initViews() {

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

        viewModel.transactionFeeLiveData.observe(this, object : Observer<Long> {
            override fun onChanged(value: Long?) {
                value?.let {
                    estimated_transaction_fee.text = getString(R.string.delegation_register_delegation_amount_estimated_transaction_fee, CurrencyUtil.formatGTU(value))
                }
            }
        })

        val authenticationText = authenticateText(viewModel.shouldUseBiometrics(), viewModel.usePasscode())
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(authenticationText, viewModel.shouldUseBiometrics(), viewModel.usePasscode(), object : AuthenticationCallback{
                        override fun getCipherForBiometrics() : Cipher?{
                            return viewModel.getCipherForBiometrics()
                        }
                        override fun onCorrectPassword(password: String) {
                            viewModel.continueWithPassword(password)
                        }
                        override fun onCipher(cipher: Cipher) {
                            viewModel.checkLogin(cipher)
                        }
                        override fun onCancelled() {
                        }
                    })
                }
            }
        })

        viewModel.loadTransactionFee()
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

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
            submit_delegation_transaction.isEnabled = false
        } else {
            progress_layout.visibility = View.GONE
            submit_delegation_transaction.isEnabled = true
        }
    }
}
