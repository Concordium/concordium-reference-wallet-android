package com.concordium.wallet

import android.app.Application
import android.content.Context
import com.concordium.wallet.util.Log
import com.walletconnect.android.RelayClient
import com.walletconnect.android.connection.ConnectionType
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

    private fun initialize(){
        appContext = this
        appCore = AppCore(this.applicationContext)
        initWalletConnect()
    }

    private fun initWalletConnect() {
        val projectId = "76324905a70fe5c388bab46d3e0564dc"
        val relayServerUrl = "wss://relay.walletconnect.com?projectId=$projectId"

        RelayClient.initialize(relayServerUrl = relayServerUrl, connectionType = ConnectionType.AUTOMATIC, application = this)

        val initString = Sign.Params.Init(
            metadata = Sign.Model.AppMetaData(
                name = "Concordium",
                description = "Concordium Wallet",
                url = "https://concordium.com",
                icons = listOf(),
                redirect = "kotlin-wallet-wc:/request"
            ),
            relay = RelayClient
        )

        SignClient.initialize(initString) { modelError ->
            println("LC -> INIT ${modelError.throwable.stackTraceToString()}")
        }
    }
}
