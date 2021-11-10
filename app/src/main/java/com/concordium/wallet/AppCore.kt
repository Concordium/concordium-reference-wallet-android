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
import com.concordium.wallet.data.model.IdentityCreationData
import com.concordium.wallet.data.model.RawJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder


class AppCore(val context: Context) {

    val gson: Gson = initializeGson()
    val proxybackendConfig = ProxyBackendConfig(gson)
    val cryptoLibrary: CryptoLibrary
    val session: Session = Session(App.appContext)

    private val authenticationManagerGeneric: AuthenticationManager = AuthenticationManager(session.getBiometricAuthKeyName())
    private var authenticationManagerReset: AuthenticationManager = authenticationManagerGeneric
    private var authenticationManager: AuthenticationManager = authenticationManagerGeneric
    private var resetBiometricKeyNameAppendix: String = ""

    // Have to keep this intent data in case the Activity is force killed while on the IdentityProvider website
    var identityCreationData: IdentityCreationData? = null

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

    public fun getOriginalAuthenticationManager() : AuthenticationManager {
        return authenticationManagerReset
    }

    public fun getCurrentAuthenticationManager() : AuthenticationManager {
        return authenticationManager
    }

    public fun startResetAuthFlow(){
        resetBiometricKeyNameAppendix = System.currentTimeMillis().toString()
        authenticationManagerReset = AuthenticationManager(resetBiometricKeyNameAppendix)
        authenticationManager = authenticationManagerReset
    }

    public fun finalizeResetAuthFlow(){
        session.setBiometricAuthKeyName(resetBiometricKeyNameAppendix)
        session.hasFinishedSetupPassword()
    }

    public fun cancelResetAuthFlow(){
        authenticationManagerReset = authenticationManagerGeneric
        authenticationManager = authenticationManagerReset
        session.hasFinishedSetupPassword()
    }
}