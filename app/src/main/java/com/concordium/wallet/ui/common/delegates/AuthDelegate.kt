package com.concordium.wallet.ui.common.delegates

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.uicore.dialog.AuthenticationDialogFragment
import kotlinx.coroutines.*
import javax.crypto.Cipher

interface AuthDelegate {
    fun showAuthentication(activity: AppCompatActivity, authenticated: (String?) -> Unit)
}

class AuthDelegateImpl : AuthDelegate {
    override fun showAuthentication(activity: AppCompatActivity, authenticated: (String?) -> Unit) {
        if (App.appCore.getCurrentAuthenticationManager().useBiometrics()) {
            showBiometrics(activity, authenticated)
        } else {
            showPasswordDialog(activity, authenticated)
        }
    }

    private fun showBiometrics(activity: AppCompatActivity, authenticated: (String?) -> Unit) {
        val biometricPrompt = createBiometricPrompt(activity, authenticated)

        val promptInfo = createPromptInfo(activity)

        val cipher = App.appCore.getCurrentAuthenticationManager().initBiometricsCipherForDecryption()
        if (cipher != null) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun showPasswordDialog(activity: AppCompatActivity, authenticated: (String?) -> Unit) {
        val dialogFragment = AuthenticationDialogFragment()
        dialogFragment.setCallback(object : AuthenticationDialogFragment.Callback {
            override fun onCorrectPassword(password: String) {
                authenticated(password)
            }
            override fun onCancelled() {
            }
        })
        dialogFragment.show(activity.supportFragmentManager, AuthenticationDialogFragment.AUTH_DIALOG_TAG)
    }

    private fun createBiometricPrompt(activity: AppCompatActivity, authenticated: (String?) -> Unit): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPromptCallback() {
            override fun onNegativeButtonClicked() {
                showPasswordDialog(activity, authenticated)
            }

            override fun onAuthenticationSucceeded(cipher: Cipher) {
                CoroutineScope(Dispatchers.IO).launch {
                    val password = App.appCore.getCurrentAuthenticationManager().checkPasswordInBackground(cipher)
                    authenticated(password)
                }
            }

            override fun onUserCancelled() {
            }
        }

        return BiometricPrompt(activity, executor, callback)
    }

    private fun createPromptInfo(activity: AppCompatActivity): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.auth_login_biometrics_dialog_title))
            .setConfirmationRequired(true)
            .setNegativeButtonText(activity.getString(if (App.appCore.getCurrentAuthenticationManager().usePasscode()) R.string.auth_login_biometrics_dialog_cancel_passcode else R.string.auth_login_biometrics_dialog_cancel_password))
            .build()
    }
}
