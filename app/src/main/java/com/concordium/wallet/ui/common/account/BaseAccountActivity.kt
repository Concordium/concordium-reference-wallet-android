package com.concordium.wallet.ui.common.account

import android.content.Intent
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.account.common.NewAccountViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.failed.FailedActivity
import com.concordium.wallet.ui.common.failed.FailedViewModel
import com.concordium.wallet.uicore.dialog.AuthenticationDialogFragment
import javax.crypto.Cipher

abstract class BaseAccountActivity : BaseActivity() {

    protected lateinit var viewModelNewAccount: NewAccountViewModel
    private lateinit var biometricPrompt: BiometricPrompt

    protected fun initializeNewAccountViewModel() {
        viewModelNewAccount = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NewAccountViewModel::class.java]
    }

    protected fun initializeAuthenticationObservers() {
        viewModelNewAccount.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) showAuthentication()
            }
        })
        viewModelNewAccount.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModelNewAccount.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModelNewAccount.gotoAccountCreatedLiveData.observe(this, object : EventObserver<Account>() {
            override fun onUnhandledEvent(value: Account) {
                accountCreated(value)
            }
        })
        viewModelNewAccount.gotoFailedLiveData.observe(this, object : EventObserver<Pair<Boolean, BackendError?>>() {
            override fun onUnhandledEvent(value: Pair<Boolean, BackendError?>) {
                if (value.first) {
                    gotoFailed(value.second)
                }
            }
        })
    }

    protected abstract fun showWaiting(waiting: Boolean)
    protected abstract fun showError(stringRes: Int)
    protected abstract fun accountCreated(account: Account)

    protected fun showAuthentication() {
        if (viewModelNewAccount.shouldUseBiometrics()) {
            showBiometrics()
        } else {
            showPasswordDialog()
        }
    }

    protected fun showPasswordDialog() {
        val dialogFragment = AuthenticationDialogFragment()
        dialogFragment.setCallback(object : AuthenticationDialogFragment.Callback {
            override fun onCorrectPassword(password: String) {
                viewModelNewAccount.continueWithPassword(password)
            }
            override fun onCancelled() {
            }
        })
        dialogFragment.show(supportFragmentManager, AuthenticationDialogFragment.AUTH_DIALOG_TAG)
    }

    protected fun gotoFailed(error: BackendError?) {
        val intent = Intent(this, FailedActivity::class.java)
        intent.putExtra(FailedActivity.EXTRA_SOURCE, FailedViewModel.Source.Account)
        error?.let {
            intent.putExtra(FailedActivity.EXTRA_ERROR, it)
        }
        startActivity(intent)
    }

    private fun showBiometrics() {
        biometricPrompt = createBiometricPrompt()

        val promptInfo = createPromptInfo()

        val cipher = viewModelNewAccount.getCipherForBiometrics()
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
                viewModelNewAccount.checkLogin(cipher)
            }

            override fun onUserCancelled() {
            }
        }

        return BiometricPrompt(this, executor, callback)
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_login_biometrics_dialog_title))
            .setConfirmationRequired(true)
            .setNegativeButtonText(getString(if (viewModelNewAccount.usePasscode()) R.string.auth_login_biometrics_dialog_cancel_passcode else R.string.auth_login_biometrics_dialog_cancel_password))
            .build()
    }
}
