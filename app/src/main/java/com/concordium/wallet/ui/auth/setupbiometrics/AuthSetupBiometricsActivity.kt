package com.concordium.wallet.ui.auth.setupbiometrics

import android.app.Activity
import android.os.Bundle
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_auth_setup_biometrics.*
import javax.crypto.Cipher

class AuthSetupBiometricsActivity :
    BaseActivity(R.layout.activity_auth_setup_biometrics, R.string.auth_setup_biometrics_title) {

    private lateinit var viewModel: AuthSetupBiometricsViewModel
    private lateinit var biometricPrompt: BiometricPrompt


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()

        biometricPrompt = createBiometricPrompt()
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
        ).get(AuthSetupBiometricsViewModel::class.java)

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        })
    }

    private fun initializeViews() {
        hideActionBarBack(this)
        enable_biometrics_button.setOnClickListener {
            onEnableBiometricsClicked()
        }
        cancel_button.setOnClickListener {
            onCancelClicked()
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun onEnableBiometricsClicked() {
        val promptInfo = createPromptInfo()

        val cipher = viewModel.getCipherForBiometrics()
        if (cipher != null) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun onCancelClicked() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(root_layout, stringRes)
    }

    //endregion

    //region Biometrics
    //************************************************************

    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPromptCallback() {
            override fun onAuthenticationSucceeded(cipher: Cipher) {
                viewModel.setupBiometricWithPassword(cipher)
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        return biometricPrompt
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_setup_biometrics_dialog_title))
            .setSubtitle(getString(R.string.auth_setup_biometrics_dialog_subtitle))
            .setConfirmationRequired(false)
            .setNegativeButtonText(getString(R.string.auth_setup_biometrics_dialog_cancel))
            .build()
        return promptInfo
    }

    //endregion
}
