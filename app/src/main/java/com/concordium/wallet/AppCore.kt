package com.concordium.wallet

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.concordium.wallet.core.authentication.AuthenticationManager
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.core.crypto.CryptoLibraryMock
import com.concordium.wallet.core.crypto.CryptoLibraryReal
import com.concordium.wallet.core.gson.BigIntegerTypeAdapter
import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.backend.ProxyBackend
import com.concordium.wallet.data.backend.ProxyBackendConfig
import com.concordium.wallet.data.model.RawJson
import com.concordium.wallet.data.room.Identity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Cookie
import java.math.BigInteger

class AppCore(val context: Context) {

    val gson: Gson = initializeGson()
    val proxyBackendConfig = ProxyBackendConfig(context, gson)
    val cryptoLibrary: CryptoLibrary =
        if (BuildConfig.USE_LIB_MOCK) CryptoLibraryMock(gson) else CryptoLibraryReal(gson)
    val session: Session = Session(App.appContext)
    var closingPoolsChecked = false
    var cookies: List<Cookie> = emptyList()
    var newIdentities = mutableMapOf<Int, Identity>()

    private val authenticationManagerGeneric: AuthenticationManager =
        AuthenticationManager(session.getBiometricAuthKeyName())
    private var authenticationManagerReset: AuthenticationManager = authenticationManagerGeneric
    private var authenticationManager: AuthenticationManager = authenticationManagerGeneric
    private var resetBiometricKeyNameAppendix: String = ""

    init {
        authenticationManager.verifyValidBiometricKeyStore()
    }

    fun getProxyBackend(): ProxyBackend {
        return proxyBackendConfig.backend
    }

    private fun initializeGson(): Gson {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(RawJson::class.java, RawJsonTypeAdapter())
        gsonBuilder.registerTypeAdapter(BigInteger::class.java, BigIntegerTypeAdapter())
        return gsonBuilder.create()
    }

    fun getOriginalAuthenticationManager(): AuthenticationManager {
        return authenticationManagerReset
    }

    fun getCurrentAuthenticationManager(): AuthenticationManager {
        return authenticationManager
    }

    fun startResetAuthFlow() {
        resetBiometricKeyNameAppendix = System.currentTimeMillis().toString()
        authenticationManagerReset = AuthenticationManager(resetBiometricKeyNameAppendix)
        authenticationManager = authenticationManagerReset
    }

    fun finalizeResetAuthFlow() {
        session.setBiometricAuthKeyName(resetBiometricKeyNameAppendix)
        session.hasFinishedSetupPassword()
    }

    fun cancelResetAuthFlow() {
        authenticationManagerReset = authenticationManagerGeneric
        authenticationManager = authenticationManagerReset
        session.hasFinishedSetupPassword()
    }

    fun getAppVersion(): Int {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val longVersionCode = PackageInfoCompat.getLongVersionCode(pInfo)
        return longVersionCode.toInt()
    }
}
