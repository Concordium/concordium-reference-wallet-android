package com.concordium.wallet.uicore

import android.text.Editable
import android.text.TextWatcher
import com.concordium.wallet.data.util.CurrencyUtil

class MaxAmountTextWatcher() : TextWatcher {

    private var previousText: String = ""

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

    override fun afterTextChanged(editable: Editable) {
        var change = false
        var str = editable.toString()
        str.replace("Ͼ", "").also { str = it }

        try {
            val intVal = CurrencyUtil.getWholePart(str)
            if (intVal != null) {
                if (intVal > (Long.MAX_VALUE - 999999L) / 1000000L) {
                    change = true
                }
            }
        }
        catch (ex: Exception) {
            change = true
        }

        if (change)
            editable.replace(0, editable.length, previousText)
        else
            previousText = editable.toString()
    }
}
