package com.concordium.wallet.ui.auth.login

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.uicore.view.PasscodeView
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_auth_login.*
import kotlinx.android.synthetic.main.progress.*
import javax.crypto.Cipher

class AuthLoginActivity : BaseActivity(R.layout.activity_auth_login, R.string.auth_login_title) {

    private lateinit var viewModel: AuthLoginViewModel
    private lateinit var biometricPrompt: BiometricPrompt


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()

        if (viewModel.shouldShowBiometrics()) {
            showBiometrics()
        }
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(AuthLoginViewModel::class.java)

        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    finish()
                }
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
        viewModel.passwordErrorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showPasswordError(value)
            }
        })
    }

    private fun initializeViews() {
        showWaiting(false)
        hideActionBarBack(this)
        setActionBarTitle(if (viewModel.usePasscode()) R.string.auth_login_info_passcode else R.string.auth_login_info_password)
        confirm_button.setOnClickListener {
            onConfirmClicked()
        }
        if (viewModel.usePasscode()) {
            password_edittext.visibility = View.GONE
            confirm_button.visibility = View.INVISIBLE
            passcode_view.passcodeListener = object : PasscodeView.PasscodeListener {
                override fun onInputChanged() {
                    error_textview.setText("")
                }

                override fun onDone() {
                    onConfirmClicked()
                }
            }
            passcode_view.requestFocus()
        } else {
            passcode_view.visibility = View.GONE
            password_edittext.setOnEditorActionListener { _, actionId, _ ->
                return@setOnEditorActionListener when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        onConfirmClicked()
                        true
                    }
                    else -> false
                }
            }
            password_edittext.afterTextChanged {
                error_textview.setText("")
            }
            password_edittext.requestFocus()
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    private fun onConfirmClicked() {
        if (viewModel.usePasscode()) {
            viewModel.checkLogin(passcode_view.getPasscode())
        } else {
            viewModel.checkLogin(password_edittext.text.toString())
        }
    }

    private fun showBiometrics() {
        biometricPrompt = createBiometricPrompt()

        val promptInfo = createPromptInfo()

        val cipher = viewModel.getCipherForBiometrics()
        if (cipher != null) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun showPasswordError(stringRes: Int) {
        password_edittext.setText("")
        passcode_view.clearPasscode()
        error_textview.setText(stringRes)
    }

    private fun showError(stringRes: Int) {
        password_edittext.setText("")
        passcode_view.clearPasscode()
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(root_layout, stringRes)
    }

    //endregion

    //region Biometrics
    //************************************************************


    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPromptCallback() {
            override fun onAuthenticationSucceeded(cipher: Cipher) {
                viewModel.checkLogin(cipher)
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        return biometricPrompt
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_login_biometrics_dialog_title))
            .setSubtitle(getString(R.string.auth_login_biometrics_dialog_subtitle))
            .setConfirmationRequired(false)
            .setNegativeButtonText(getString(if (viewModel.usePasscode()) R.string.auth_login_biometrics_dialog_cancel_passcode else R.string.auth_login_biometrics_dialog_cancel_password))
            .build()
        return promptInfo
    }

    override fun loggedOut() {
        // do nothing as we are one of the root activities
    }

    override fun loggedIn() {
        finish()
    }
    //endregion

}
