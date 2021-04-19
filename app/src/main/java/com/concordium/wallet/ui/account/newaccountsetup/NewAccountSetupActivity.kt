package com.concordium.wallet.ui.account.newaccountsetup

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.account.newaccountconfirmed.NewAccountConfirmedActivity
import com.concordium.wallet.ui.account.newaccountidentityattributes.NewAccountIdentityAttributesActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.failed.FailedActivity
import com.concordium.wallet.ui.common.failed.FailedViewModel
import com.concordium.wallet.uicore.dialog.AuthenticationDialogFragment
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_new_account_setup.*
import kotlinx.android.synthetic.main.progress.*
import javax.crypto.Cipher

class NewAccountSetupActivity : BaseActivity(
    R.layout.activity_new_account_setup,
    R.string.new_account_setup_title
) {

    companion object {
        const val EXTRA_ACCOUNT_NAME = "EXTRA_ACCOUNT_NAME"
        const val EXTRA_IDENTITY = "EXTRA_IDENTITY"
    }

    private lateinit var viewModel: NewAccountSetupViewModel
    private lateinit var biometricPrompt: BiometricPrompt


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME) as String
        val identity = intent.getSerializableExtra(EXTRA_IDENTITY) as Identity

        initializeViewModel()
        viewModel.initialize(accountName, identity)
        initViews()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(NewAccountSetupViewModel::class.java)

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
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication()
                }
            }
        })
        viewModel.gotoAccountCreatedLiveData.observe(this, object : EventObserver<Account>() {
            override fun onUnhandledEvent(value: Account) {
                gotoNewAccountConfirmed(value)
            }
        })
        viewModel.gotoFailedLiveData.observe(this, object : EventObserver<Pair<Boolean, BackendError?>>() {
            override fun onUnhandledEvent(value: Pair<Boolean, BackendError?>) {
                if (value.first) {
                    gotoFailed(value.second)
                }
            }
        })
    }

    private fun initViews() {
        progress_layout.visibility = View.GONE

        confirm_reveal_button.setOnClickListener {
            gotoNewAccountIdentityAttributes(viewModel.accountName, viewModel.identity)
        }
        confirm_submit_button.setOnClickListener {
            confirm_reveal_button.isEnabled = false
            confirm_submit_button.isEnabled = false
            viewModel.confirmWithoutAttributes()
        }
    }


    //endregion

    //region Control
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
            confirm_reveal_button.isEnabled = false
            confirm_submit_button.isEnabled = false
        } else {
            progress_layout.visibility = View.GONE
            confirm_reveal_button.isEnabled = true
            confirm_submit_button.isEnabled = true
        }
    }

    private fun showError(stringRes: Int) {
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(root_layout, stringRes)
    }

    private fun gotoNewAccountConfirmed(account: Account) {
        val intent = Intent(this, NewAccountConfirmedActivity::class.java)
        intent.putExtra(NewAccountConfirmedActivity.EXTRA_ACCOUNT, account)
        startActivity(intent)
    }

    private fun gotoFailed(error: BackendError?) {
        val intent = Intent(this, FailedActivity::class.java)
        intent.putExtra(FailedActivity.EXTRA_SOURCE, FailedViewModel.Source.Account)
        error?.let {
            intent.putExtra(FailedActivity.EXTRA_ERROR, it)
        }
        startActivity(intent)
    }

    private fun gotoNewAccountIdentityAttributes(accountName: String, identity: Identity) {
        val intent = Intent(this, NewAccountIdentityAttributesActivity::class.java)
        intent.putExtra(NewAccountIdentityAttributesActivity.EXTRA_ACCOUNT_NAME, accountName)
        intent.putExtra(NewAccountIdentityAttributesActivity.EXTRA_IDENTITY, identity)
        startActivity(intent)
    }

    private fun showAuthentication() {
        if (viewModel.shouldUseBiometrics()) {
            showBiometrics()
        } else {
            showPasswordDialog()
        }
    }

    private fun showPasswordDialog() {
        val dialogFragment = AuthenticationDialogFragment()
        dialogFragment.setCallback(object : AuthenticationDialogFragment.Callback {
            override fun onCorrectPassword(password: String) {
                viewModel.continueWithPassword(password)
            }

            override fun onCancelled() {
            }
        })
        dialogFragment.show(supportFragmentManager, AuthenticationDialogFragment.AUTH_DIALOG_TAG)
    }

    //endregion

    //region Biometrics
    //************************************************************

    private fun showBiometrics() {
        biometricPrompt = createBiometricPrompt()

        val promptInfo = createPromptInfo()

        val cipher = viewModel.getCipherForBiometrics()
        if (cipher != null) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPromptCallback() {
            override fun onNegativeButtonClicked() {
                showPasswordDialog()
            }

            override fun onAuthenticationSucceeded(cipher: Cipher) {
                viewModel.checkLogin(cipher)
            }

            override fun onUserCancelled() {
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        return biometricPrompt
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_login_biometrics_dialog_title))
            .setConfirmationRequired(true)
            .setNegativeButtonText(getString(if (viewModel.usePasscode()) R.string.auth_login_biometrics_dialog_cancel_passcode else R.string.auth_login_biometrics_dialog_cancel_password))
            .build()
        return promptInfo
    }

    //endregion
}
