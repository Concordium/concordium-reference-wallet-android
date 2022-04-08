package com.concordium.wallet.ui.bakerdelegation.delegation

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.DelegationData
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.*
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

        viewModel.showDetailedLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showConfirmationPage()
                }
            }
        })

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError()
            }
        })

    }

    private fun showError() {
        Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show()
        //pool_id.setTextColor(getColor(R.color.text_pink))
        //pool_id_error.visibility = View.VISIBLE
    }

    private fun hideError() {
        //pool_id.setTextColor(getColor(R.color.theme_blue))
        //pool_id_error.visibility = View.GONE
    }

    private fun showConfirmationPage() {

    }

    fun initViews() {
        updateVisibilities()

        pool_registration_continue.setOnClickListener {
            onContinueClicked()
        }

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

    }

    private fun updateVisibilities() {
        //pool_id.visibility = if(viewModel.isLPool()) View.GONE else View.VISIBLE
        //pool_desc.visibility = if(viewModel.isLPool()) View.GONE else View.VISIBLE
        //pool_registration_continue.isEnabled = pool_id.length() > 0
        hideError()
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
