package com.concordium.wallet.util

import android.content.res.Resources
import kotlin.math.floor

object UnitConvertUtil {

    fun convertPixelsToDp(px: Float): Float {
        val metrics = Resources.getSystem().displayMetrics
        val dp = px / (metrics.densityDpi / 160f)
        return Math.round(dp).toFloat()
    }

    fun convertDpToPixel(dp: Float): Float {
        val metrics = Resources.getSystem().displayMetrics
        val px = dp * (metrics.densityDpi / 160f)
        return Math.round(px).toFloat()
    }

    fun secondsToDaysRoundedDown(seconds: Long): Int {
        return floor((seconds.toDouble()/60/60/24)).toInt()
    }
}
