package com.concordium.wallet.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.concordium.wallet.BuildConfig

object CryptoX {
    private val packageName =
        if (BuildConfig.ENV_NAME == "prod_testnet")
            "com.pioneeringtechventures.wallet.testnet"
        else
            "com.pioneeringtechventures.wallet"

    val marketWebUrl =
        "https://play.google.com/store/apps/details?id=$packageName"

    fun openMarket(context: Context) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(marketWebUrl)
            )
        )
    }
}
