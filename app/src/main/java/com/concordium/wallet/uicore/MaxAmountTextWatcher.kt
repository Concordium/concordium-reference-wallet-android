package com.concordium.wallet.uicore

import android.text.Editable
import android.text.TextWatcher
import com.concordium.wallet.data.util.CurrencyUtil

class MaxAmountTextWatcher : TextWatcher {

    private var previousText: String = ""

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(editable: Editable) {
        val gtuValue = CurrencyUtil.toGTUValue(editable.toString().trim(), decimals = 0)
        if (gtuValue != null && gtuValue > CurrencyUtil.MAX_AMOUNT)
            editable.replace(0, editable.length, previousText)
        else
            previousText = editable.toString()
    }
}
