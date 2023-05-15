package com.concordium.wallet.uicore.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.widget.EditText
import com.concordium.wallet.uicore.DecimalTextWatcher
import com.concordium.wallet.uicore.MaxAmountTextWatcher
import java.text.DecimalFormatSymbols

@SuppressLint("AppCompatCustomView")
class AmountEditText: EditText {

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
        keyListener = DigitsKeyListener.getInstance("0123456789" + DecimalFormatSymbols.getInstance().decimalSeparator)
        addTextChangedListener(DecimalTextWatcher(6))
        addTextChangedListener(MaxAmountTextWatcher())
    }
}
