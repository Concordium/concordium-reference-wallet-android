package com.concordium.wallet.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.concordium.wallet.BuildConfig

object CryptoX {
    fun openMarket(
        context: Context,
    ) {
        val cryptoXPackage =
            if (BuildConfig.ENV_NAME == "prod_testnet")
                "com.pioneeringtechventures.wallet.testnet"
            else
                "com.pioneeringtechventures.wallet"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.parse("market://details?id=$cryptoXPackage")
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$cryptoXPackage")
                )
            )
        }
    }
}
