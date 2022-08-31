package com.concordium.wallet.ui.auth.login

import android.content.Intent
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
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.databinding.ActivityAuthLoginBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.intro.introsetup.IntroSetupActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.uicore.view.PasscodeView
import com.concordium.wallet.util.KeyboardUtil
import javax.crypto.Cipher

class AuthLoginActivity : BaseActivity() {
    private lateinit var binding: ActivityAuthLoginBinding
    private lateinit var viewModel: AuthLoginViewModel
    private lateinit var biometricPrompt: BiometricPrompt

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.auth_login_title)

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
        )[AuthLoginViewModel::class.java]
        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    KeyboardUtil.hideKeyboard(this@AuthLoginActivity)
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
        hideActionBarBack()
        setActionBarTitle(if (viewModel.usePasscode()) R.string.auth_login_info_passcode else R.string.auth_login_info_password)
        binding.confirmButton.setOnClickListener {
            onConfirmClicked()
        }
        if (viewModel.usePasscode()) {
            binding.passwordEdittext.visibility = View.GONE
            binding.confirmButton.visibility = View.INVISIBLE
            binding.passcodeView.passcodeListener = object : PasscodeView.PasscodeListener {
                override fun onInputChanged() {
                    binding.errorTextview.text = ""
                }

                override fun onDone() {
                    onConfirmClicked()
                }
            }
            binding.passcodeView.requestFocus()
        } else {
            binding.passcodeView.visibility = View.GONE
            binding.passwordEdittext.setOnEditorActionListener { _, actionId, _ ->
                return@setOnEditorActionListener when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        onConfirmClicked()
                        true
                    }
                    else -> false
                }
            }
            binding.passwordEdittext.afterTextChanged {
                binding.errorTextview.text = ""
            }
            binding.passwordEdittext.requestFocus()
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }

    private fun onConfirmClicked() {
        if (viewModel.usePasscode()) {
            viewModel.checkLogin(binding.passcodeView.getPasscode())
        } else {
            viewModel.checkLogin(binding.passwordEdittext.text.toString())
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
        binding.passwordEdittext.setText("")
        binding.passcodeView.clearPasscode()
        binding.errorTextview.setText(stringRes)
    }

    private fun showError(stringRes: Int) {
        binding.passwordEdittext.setText("")
        binding.passcodeView.clearPasscode()
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(binding.rootLayout, stringRes)
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
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_login_biometrics_dialog_title))
            .setSubtitle(getString(R.string.auth_login_biometrics_dialog_subtitle))
            .setConfirmationRequired(false)
            .setNegativeButtonText(getString(if (viewModel.usePasscode()) R.string.auth_login_biometrics_dialog_cancel_passcode else R.string.auth_login_biometrics_dialog_cancel_password))
            .build()
    }

    override fun loggedOut() {
        // do nothing as we are one of the root activities
    }

    override fun loggedIn() {
        if (!AuthPreferences(this).hasSeedPhrase()) run {
            startActivity(Intent(this, IntroSetupActivity::class.java))
        }
        finish()
    }

    //endregion
}
