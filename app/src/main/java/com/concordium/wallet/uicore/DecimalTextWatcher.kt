package com.concordium.wallet.uicore

import android.text.Editable
import android.text.TextWatcher
import java.text.DecimalFormatSymbols

class DecimalTextWatcher(val maxNumberOfDecimals: Int = 1) : TextWatcher {
    private val separator: Char = DecimalFormatSymbols.getInstance().decimalSeparator

    override fun beforeTextChanged(
        charSequence: CharSequence,
        i: Int,
        i1: Int,
        i2: Int
    ) {
    }

    override fun onTextChanged(
        charSequence: CharSequence,
        i: Int,
        i1: Int,
        i2: Int
    ) {
    }

    override fun afterTextChanged(editable: Editable) {
        var change = false
        var str = editable.toString()
        // Replace the non decimal separator with the decimal separator
        if ("." != "" + separator) {
            if (str.indexOf(".") > -1) {
                // Only trigger change if there are any (to avoid loop)
                str = str.replace('.', separator)
                change = true
            }
        } else if ("," != "" + separator) {
            if (str.indexOf(",") > -1) {
                // Only trigger change if there are any (to avoid loop)
                str = str.replace(',', separator)
                change = true
            }
        }
        val decimalSeparatorIndex = str.indexOf(separator)
        // Decimal operator in the first space is not allowed - remove it
        if (decimalSeparatorIndex == 0) {
            str = str.substring(1)
            change = true
        }
        if (decimalSeparatorIndex > 0) {
            // Delete decimal operator occurrences after the first
            if (str.lastIndexOf(separator) != decimalSeparatorIndex) {
                // Replace the last char with the empty string
                str = str.substring(0, str.length - 1)
                change = true
            }
            // Delete the last char if there is more than 'maxNumberOfDecimals' decimal after the decimal separator
            val indexOfLastChar = str.length - 1
            if (indexOfLastChar > decimalSeparatorIndex + maxNumberOfDecimals) { // too many decimals (not allowed)
                // Replace the last char with the empty string
                str = str.substring(0, str.length - 1)
                change = true
            }
        }
        if (change) {
            editable.replace(0, editable.length, str)
        }
    }
}