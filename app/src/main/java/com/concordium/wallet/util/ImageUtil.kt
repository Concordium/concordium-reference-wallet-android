package com.concordium.wallet.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import androidx.core.content.ContextCompat

object ImageUtil {

    fun getImageBitmap(image: String): Bitmap {
        val imageBytes = Base64.decode(image, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(
            imageBytes, 0, imageBytes.size
        )
    }

    fun changeImageViewTintColor(imageView: ImageView, color: Int) {
        imageView.setColorFilter(
            ContextCompat.getColor(imageView.context, color),
            android.graphics.PorterDuff.Mode.MULTIPLY
        )
    }
}