package com.concordium.wallet.util

import android.content.res.Resources
import kotlin.math.ceil
import kotlin.math.floor

object UnitConvertUtil {
    fun convertPixelsToDp(px: Float): Int {
        val metrics = Resources.getSystem().displayMetrics
        val dp = px / (metrics.densityDpi / 160f)
        return ceil(dp).toInt()
    }

    fun convertDpToPixel(dp: Float): Int {
        val metrics = Resources.getSystem().displayMetrics
        val px = dp * (metrics.densityDpi / 160f)
        return ceil(px).toInt()
    }

    fun secondsToDaysRoundedDown(seconds: Long): Int {
        return floor((seconds.toDouble() / 60 / 60 / 24)).toInt()
    }
}
