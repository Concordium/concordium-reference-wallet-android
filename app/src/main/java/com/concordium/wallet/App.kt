package com.concordium.wallet

import android.app.Application
import android.content.Context
import com.concordium.wallet.util.Log

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
    }


}
