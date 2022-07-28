package com.concordium.wallet.ui.passphrase.setup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class CrossHideView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val paintLightGray = Paint(Paint.ANTI_ALIAS_FLAG)
        paintLightGray.style = Paint.Style.FILL
        paintLightGray.color = Color.parseColor("#C4C4C4")
        canvas!!.drawRect(0f, 0f ,width.toFloat() ,height.toFloat() ,paintLightGray)

        val paintDarkGray = Paint(Paint.ANTI_ALIAS_FLAG)
        paintDarkGray.style = Paint.Style.STROKE
        paintDarkGray.strokeWidth = 3f
        paintDarkGray.color = Color.parseColor("#979797")

        val path = Path()
        path.moveTo(0f, 0f)
        path.lineTo(width.toFloat(), 0f)
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.lineTo(0f, 0f)
        path.lineTo(width.toFloat(), height.toFloat())
        path.moveTo(width.toFloat(), 0f)
        path.lineTo(0f, height.toFloat())
        canvas.drawPath(path, paintDarkGray)
    }
}
