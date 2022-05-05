package com.concordium.wallet.ui.bakerdelegation.common

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_delegation_remove.*
import kotlinx.android.synthetic.main.progress.*
import javax.crypto.Cipher

abstract class BaseDelegationBakerActivity(layout: Int, titleId: Int = R.string.app_name) :
    BaseActivity(layout, titleId) {

    protected lateinit var viewModel: DelegationBakerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize(intent.extras?.getSerializable(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA) as BakerDelegationData)
        initViews()
    }

    open fun initializeViewModel() {
        showWaiting(false)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(DelegationBakerViewModel::class.java)
    }

    protected fun initializeWaitingLiveData() {
        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                errorLiveData(value)
            }
        })
    }

    protected fun initializeTransactionFeeLiveData() {
        viewModel.transactionFeeLiveData.observe(this, object : Observer<Pair<Long?, Int?>> {
            override fun onChanged(response: Pair<Long?, Int?>?) {
                response?.first?.let {
                    estimated_transaction_fee.text = getString(R.string.delegation_register_delegation_amount_estimated_transaction_fee, CurrencyUtil.formatGTU(it))
                }
            }
        })
        viewModel.loadTransactionFee(false)
    }

    protected fun initializeShowAuthenticationLiveData() {
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
    }

    abstract fun errorLiveData(value: Int)

    protected open fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    protected fun showNotEnoughFunds() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_remove_not_enough_funds_title)
        builder.setMessage(getString(R.string.delegation_remove_not_enough_funds_message))
        builder.setPositiveButton(getString(R.string.delegation_remove_not_enough_funds_ok)) { dialog, _ ->
            dialog.dismiss()
            finishUntilClass(AccountDetailsActivity::class.java.canonicalName)
        }
        builder.create().show()
    }

    protected fun showNoChange() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_no_changes_title)
        builder.setMessage(getString(R.string.delegation_no_changes_message))
        builder.setPositiveButton(getString(R.string.delegation_no_changes_ok)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    protected open fun initViews() { }
}
