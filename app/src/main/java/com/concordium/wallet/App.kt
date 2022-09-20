package com.concordium.wallet

import android.app.Application
import android.content.Context
import com.concordium.wallet.util.Log
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

        val initString = Sign.Params.Init(
            application = this,
            relayServerUrl = relayServerUrl,
            metadata = Sign.Model.AppMetaData(
                name = "Concordium Wallet",
                description = "Concordium Wallet description",
                url = "https://concordium.com",
                icons = listOf("https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media"),
                redirect = "kotlin-wallet-wc:/request"
            )
        )

        println("LC -> CALL INIT")
        SignClient.initialize(initString) { modelError ->
            println("LC -> INIT ${modelError.throwable.stackTraceToString()}")
        }
    }
}
