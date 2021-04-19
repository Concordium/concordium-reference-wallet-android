package com.concordium.wallet.util

import android.content.Context
import java.io.IOException
import java.nio.charset.Charset

object AssetUtil {

    fun loadFromAsset(context: Context, asset: String): String {
        var content: String?
        try {
            val inputStream = context.getAssets().open(asset)
            val size = inputStream.available()
            val buffer = ByteArray(size)

            inputStream.read(buffer)
            inputStream.close()

            content = String(buffer, Charset.forName("UTF-8"))

        } catch (ex: IOException) {
            ex.printStackTrace()
            return ""
        }

        return content
    }
}