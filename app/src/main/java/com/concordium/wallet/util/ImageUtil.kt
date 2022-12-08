package com.concordium.wallet.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

object ImageUtil {
    fun getImageBitmap(image: String): Bitmap {
        val imageBytes = Base64.decode(image, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
