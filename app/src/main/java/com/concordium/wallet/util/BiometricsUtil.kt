package com.concordium.wallet.util

import androidx.biometric.BiometricManager
import com.concordium.wallet.App

object BiometricsUtil {
    fun isBiometricsAvailable(): Boolean {
        val biometricManager = BiometricManager.from(App.appContext)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d("App can authenticate using biometrics.")
                return true
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.e("No biometric features available on this device.")
                return false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.e("Biometric features are currently unavailable.")
                return false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.e("The user hasn't associated any biometric credentials with their account.")
                return false
            }
        }
        return false
    }
}
