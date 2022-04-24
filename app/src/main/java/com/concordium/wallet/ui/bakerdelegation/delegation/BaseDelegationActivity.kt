package com.concordium.wallet.ui.bakerdelegation.delegation

import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import kotlinx.android.synthetic.main.activity_delegation_remove.*
import javax.crypto.Cipher

abstract class BaseDelegationActivity(layout: Int, titleId: Int = R.string.app_name) :
    BaseDelegationBakerActivity(layout, titleId) {

    protected fun initializeTransactionLiveData() {
        viewModel.transactionSuccessLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                transactionSuccessLiveData()
            }
        })
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

    protected fun initializeShowDetailedLiveData() {
        viewModel.showDetailedLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                showDetailedLiveData(value)
            }
        })
    }

    protected fun initializeTransactionFeeLiveData() {
        viewModel.transactionFeeLiveData.observe(this,
            Observer<Long> { value ->
                value?.let {
                    estimated_transaction_fee.text = getString(R.string.delegation_register_delegation_amount_estimated_transaction_fee, CurrencyUtil.formatGTU(value))
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

    abstract fun transactionSuccessLiveData()
    abstract fun errorLiveData(value: Int)
    abstract fun showDetailedLiveData(value: Boolean)
}
