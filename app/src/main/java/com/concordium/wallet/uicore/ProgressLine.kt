package com.concordium.wallet.uicore

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.concordium.wallet.R

class ProgressLine : View {
    private var filled = 1
    private var numberOfDots = 4
    private val paint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.theme_black, null)
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
    }

    constructor (context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressLine, 0, 0)
            try {
                numberOfDots = typedArray.getInt(R.styleable.ProgressLine_numberOfDots, 4)
                filled = typedArray.getInt(R.styleable.ProgressLine_filledDots, 1)
            } finally {
                typedArray.recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        paint.strokeWidth = height / 5f

        val lineThickness = (height / 5f)
        val cy = (height / 2).toFloat()
        var cx = cy
        var x = height.toFloat()
        val y = cy
        val dotRadius = (height.toFloat() / 2) - (lineThickness / 2)
        val dotSpacing = (width - (height * numberOfDots)).toFloat() / (numberOfDots - 1)

        canvas?.let {
            for (i in 1..numberOfDots) {
                if (i <= filled)
                    paint.style = Paint.Style.FILL_AND_STROKE
                else
                    paint.style = Paint.Style.STROKE
                it.drawCircle(cx, cy, dotRadius, paint)
                if (i < numberOfDots) {
                    it.drawLine(x - 1, y, x + dotSpacing + 1, cy, paint)
                }
                x += dotSpacing + height
                cx = x - cy
            }
        }
    }

    fun setFilledDots(filledDots: Int) {
        filled = filledDots
    }
}
