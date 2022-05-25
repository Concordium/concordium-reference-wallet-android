package com.concordium.wallet

import android.content.Context
import com.concordium.wallet.core.authentication.AuthenticationManager
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.core.crypto.CryptoLibraryMock
import com.concordium.wallet.core.crypto.CryptoLibraryReal
import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.backend.ProxyBackend
import com.concordium.wallet.data.backend.ProxyBackendConfig
import com.concordium.wallet.data.model.RawJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class AppCore(val context: Context) {

    val gson: Gson = initializeGson()
    val proxybackendConfig = ProxyBackendConfig(gson)
    val cryptoLibrary: CryptoLibrary
    val session: Session = Session(App.appContext)
    var closingPoolsChecked = false
    var sessionCookie: String? = null

    private val authenticationManagerGeneric: AuthenticationManager = AuthenticationManager(session.getBiometricAuthKeyName())
    private var authenticationManagerReset: AuthenticationManager = authenticationManagerGeneric
    private var authenticationManager: AuthenticationManager = authenticationManagerGeneric
    private var resetBiometricKeyNameAppendix: String = ""

    init {
        cryptoLibrary =
            if (BuildConfig.USE_LIB_MOCK) CryptoLibraryMock(gson) else CryptoLibraryReal(gson)
        authenticationManager.verifyValidBiometricKeyStore()
    }

    fun getProxyBackend(): ProxyBackend {
        return proxybackendConfig.backend
    }

    private fun initializeGson(): Gson {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(RawJson::class.java, RawJsonTypeAdapter());
        return gsonBuilder.create();
    }

    fun getOriginalAuthenticationManager() : AuthenticationManager {
        return authenticationManagerReset
    }

    fun getCurrentAuthenticationManager() : AuthenticationManager {
        return authenticationManager
    }

    fun startResetAuthFlow(){
        resetBiometricKeyNameAppendix = System.currentTimeMillis().toString()
        authenticationManagerReset = AuthenticationManager(resetBiometricKeyNameAppendix)
        authenticationManager = authenticationManagerReset
    }

    fun finalizeResetAuthFlow(){
        session.setBiometricAuthKeyName(resetBiometricKeyNameAppendix)
        session.hasFinishedSetupPassword()
    }

    fun cancelResetAuthFlow(){
        authenticationManagerReset = authenticationManagerGeneric
        authenticationManager = authenticationManagerReset
        session.hasFinishedSetupPassword()
    }
}