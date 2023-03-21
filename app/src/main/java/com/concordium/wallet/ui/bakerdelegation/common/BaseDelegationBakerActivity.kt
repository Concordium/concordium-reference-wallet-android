package com.concordium.wallet.ui.bakerdelegation.common

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.util.CurrencyUtilImpl
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.base.BaseActivity
import javax.crypto.Cipher

abstract class BaseDelegationBakerActivity : BaseActivity() {
    protected lateinit var viewModel: DelegationBakerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize(intent.extras?.getSerializable(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA) as BakerDelegationData)
    }

    open fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[DelegationBakerViewModel::class.java]
    }

    protected fun initializeWaitingLiveData(progressLayout: View) {
        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(progressLayout, waiting)
            }
        }
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                errorLiveData(value)
            }
        })
    }

    protected fun initializeTransactionFeeLiveData(progressLayout: View, estimatedTransactionFee: TextView) {
        viewModel.transactionFeeLiveData.observe(this) { response ->
            response?.first?.let {
                estimatedTransactionFee.text =
                    getString(R.string.delegation_register_delegation_amount_estimated_transaction_fee,
                        CurrencyUtilImpl.formatGTU(it))
                showWaiting(progressLayout, false)
            }
        }
        viewModel.loadTransactionFee(false)
    }

    protected fun initializeShowAuthenticationLiveData() {
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(authenticateText(), object : AuthenticationCallback{
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

    protected open fun showWaiting(progressLayout: View, waiting: Boolean) {
        if (waiting) {
            progressLayout.visibility = View.VISIBLE
        } else {
            progressLayout.visibility = View.GONE
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
