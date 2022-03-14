package com.concordium.wallet.uicore.view

import android.content.Context
import android.text.InputFilter
import android.text.Spanned
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import com.concordium.wallet.R


class MaxShakeTextInputEditText: com.google.android.material.textfield.TextInputEditText {

    constructor (context: Context) : super(context) {
        init(null)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun init(attrs: AttributeSet?) {

        var maxLength = Integer.MAX_VALUE
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.MaxShakeTextInputEditText, 0, 0)
            try {
                maxLength = ta.getInt(R.styleable.MaxShakeTextInputEditText_maxLength, Integer.MAX_VALUE)
            } finally {
                ta.recycle()
            }
        }

        filters = arrayOf(object : InputFilter {
            override fun filter(
                source: CharSequence?,
                start: Int,
                end: Int,
                dest: Spanned,
                dstart: Int,
                dend: Int
            ): CharSequence? {
                //Main code from LengthFilter
                var keep: Int = maxLength - (dest.length - (dend - dstart))
                return if (keep <= 0) {
                    startAnimation(AnimationUtils.loadAnimation(context, R.anim.anim_shake))
                    ""
                } else if (keep >= end - start) {
                    null
                } else {
                    keep += start
                    if (Character.isHighSurrogate(source!![keep - 1])) {
                        --keep
                        if (keep == start) {
                            return ""
                        }
                    }
                    source?.subSequence(start, keep)
                }
            }
        })
    }

}