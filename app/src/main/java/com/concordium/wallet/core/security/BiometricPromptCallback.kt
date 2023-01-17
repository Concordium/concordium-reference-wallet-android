package com.concordium.wallet.core.security

import androidx.biometric.BiometricPrompt
import com.concordium.wallet.util.Log
import javax.crypto.Cipher

open class BiometricPromptCallback : BiometricPrompt.AuthenticationCallback() {
    companion object{
        const val ERROR_NEGATIVE_BUTTON = 13
        const val ERROR_USER_CANCELED = 10
    }
    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        Log.d("Biometrics auth error (code: $errorCode): $errString")
        if (errorCode == ERROR_NEGATIVE_BUTTON) {
            onNegativeButtonClicked()
        } else if (errorCode == ERROR_USER_CANCELED) {
            onUserCancelled()
        }
    }

    override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        Log.d("Biometric not recognized")
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        Log.d("Biometric was recognized")
        result.cryptoObject?.cipher?.let {
            onAuthenticationSucceeded(it)
            return
        }
        Log.d("Cipher not present")
    }

    open fun onNegativeButtonClicked() {

    }

    open fun onAuthenticationSucceeded(cipher: Cipher) {

    }

    open fun onUserCancelled() {

    }
}