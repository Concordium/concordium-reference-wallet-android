package com.concordium.wallet

import android.content.Context
import com.concordium.wallet.onboarding.ui.di.AppModule
import com.concordium.wallet.onboarding.ui.di.OnboardingModule
import com.concordium.wallet.util.Log
import com.walletconnect.android.BuildConfig
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.matomo.sdk.TrackerBuilder
import org.matomo.sdk.extra.MatomoApplication

class App : MatomoApplication() {

    companion object {
        lateinit var appContext: Context
        lateinit var appCore: AppCore
    }

    override fun onCreateTrackerConfig(): TrackerBuilder {
        return TrackerBuilder.createDefault("https://concordium.matomo.cloud/matomo.php", 5)
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("App starting - setting Log silent if release")
        Log.setSilent(!BuildConfig.DEBUG)
        Log.d("Log is not silent")

        initialize()
    }

    private fun initialize() {
        appContext = this
        appCore = AppCore(this.applicationContext)
        initWalletConnect()
        initKoin()
    }

    private fun initKoin() {
        val modules = AppModule()
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                modules.configModule,
                modules.sharedPreferencesModule,
                modules.remoteModule,
                modules.apiModule,
            )
            modules(OnboardingModule.modules)
        }
    }

    private fun initWalletConnect() {
        println("LC -> CALL INIT")

        val projectId = "76324905a70fe5c388bab46d3e0564dc"
        val relayServerUrl = "wss://relay.walletconnect.com?projectId=$projectId"
        val appMetaData = Core.Model.AppMetaData(
            name = getString(R.string.app_name),
            description = "Concordium - Blockchain Wallet",
            url = "https://concordium.com",
            icons = listOf(),
            redirect = "kotlin-wallet-wc:/request"
        )

        CoreClient.initialize(
            relayServerUrl = relayServerUrl,
            connectionType = ConnectionType.AUTOMATIC,
            application = this,
            metaData = appMetaData
        )
        val initParams = Sign.Params.Init(core = CoreClient)

        SignClient.initialize(initParams) { modelError ->
            println("LC -> INIT ERROR ${modelError.throwable.stackTraceToString()}")
        }
    }
}
