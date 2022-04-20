package com.concordium.wallet.ui.bakerdelegation.delegation

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.DelegationData
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_delegation_remove.*
import kotlinx.android.synthetic.main.progress.*
import javax.crypto.Cipher

abstract class BaseDelegationActivity(layout: Int, titleId: Int = R.string.app_name) :
    BaseActivity(layout, titleId) {

    protected lateinit var viewModel: DelegationViewModel

    companion object {
        const val EXTRA_DELEGATION_DATA = "EXTRA_DELEGATION_DATA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize(intent.extras?.getSerializable(EXTRA_DELEGATION_DATA) as DelegationData)
        initViews()
    }

    open fun initializeViewModel() {
        showWaiting(false)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(DelegationViewModel::class.java)
    }

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

    protected open fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
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

    protected open fun initViews() { }

    abstract fun transactionSuccessLiveData()
    abstract fun errorLiveData(value: Int)
    abstract fun showDetailedLiveData(value: Boolean)
}
