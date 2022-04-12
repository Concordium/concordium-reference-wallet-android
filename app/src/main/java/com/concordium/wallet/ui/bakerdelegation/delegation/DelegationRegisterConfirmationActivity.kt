package com.concordium.wallet.ui.bakerdelegation.delegation

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
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.UnitConvertUtil
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.pool_registration_continue
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
                Toast.makeText(this, "Transaction success!", Toast.LENGTH_SHORT).show()
                finishUntilClass(AccountDetailsActivity::class.java.canonicalName)
                //TODO show receipt screen from here
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

    fun initViews() {

        pool_registration_continue.setOnClickListener {
            onContinueClicked()
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

        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication("[confirm string]", viewModel.shouldUseBiometrics(), viewModel.usePasscode(), object : AuthenticationCallback{
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

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
            pool_registration_continue.isEnabled = false
        } else {
            progress_layout.visibility = View.GONE
            pool_registration_continue.isEnabled = true
        }
    }
}
