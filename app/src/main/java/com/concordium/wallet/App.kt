package com.concordium.wallet

import android.app.Application
import android.content.Context
import com.concordium.wallet.util.Log
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient

class App : Application(){

    companion object {
        lateinit var appContext: Context
        lateinit var appCore: AppCore
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

        CoreClient.initialize(relayServerUrl = relayServerUrl, connectionType = ConnectionType.AUTOMATIC, application = this, metaData = appMetaData)
        val initParams = Sign.Params.Init(core = CoreClient)

        SignClient.initialize(initParams) { modelError ->
            println("LC -> INIT ERROR ${modelError.throwable.stackTraceToString()}")
        }

        val pairings: List<Core.Model.Pairing> = CoreClient.Pairing.getPairings()
        println("LC -> EXISTING PAIRINGS in App = ${pairings.count()}")
        pairings.forEach { pairing ->
            CoreClient.Pairing.disconnect(pairing.topic) { modelError ->
                println("LC -> DISCONNECT ERROR ${modelError.throwable.stackTraceToString()}")
            }
        }
    }
}
