package com.concordium.wallet.data.util

import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.util.toBigDecimal
import com.concordium.wallet.util.toPlainStringStripped
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.regex.Pattern

object CurrencyUtil {
    private val separator: Char = DecimalFormatSymbols.getInstance().decimalSeparator
    private val patternGTU: Pattern = Pattern.compile("^-?[0-9]*[${separator}]?[0-9]{0,6}\$")
    private var gStroke: String? = null

    fun formatGTU(value: String, withGStroke: Boolean = false, decimals: Int = 6): String =
        formatGTU(value.toBigDecimal(), withGStroke, decimals)

    fun formatGTU(value: Long, token: Token?): String {
        var decimals = 6
        var withGStroke = true
        token?.let {
            withGStroke = it.isCCDToken
            it.tokenMetadata?.let { tokenMetadata ->
                decimals = tokenMetadata.decimals
            }
        }
        return formatGTU(value, withGStroke, decimals)
    }

    fun setGstroke(gStroke: String){
        this.gStroke = gStroke
    }

    fun formatGTU(value: Long, withGStroke: Boolean = false, decimals: Int = 6): String=
        formatGTU(value.toBigDecimal(), withGStroke, decimals)

    fun formatGTU(value: BigDecimal, withGStroke: Boolean = false, decimals: Int = 6): String {
        if(withGStroke && gStroke == null){
            gStroke = App.appContext.getString(R.string.app_gstroke)
        }
        if (decimals <= 0) {
            if (withGStroke)
                return gStroke + value.toString()
            return value.toString()
        }

        val isNegative = value.signum() < 0
        val str =
            if (isNegative)
                value.toPlainStringStripped().replace("-", "")
            else
                value.toPlainStringStripped()
        val strBuilder = StringBuilder(str)
        if (strBuilder.length <= decimals - 1) {
            // Add zeroes in front of the value until there are four chars
            while (strBuilder.length < decimals) {
                strBuilder.insert(0, "0")
            }
            // Insert 0. in front of the four chars
            strBuilder.insert(0, separator)
            strBuilder.insert(0, "0")
        } else {
            // The string is at least decimals chars - insert separator in front of the last decimals
            strBuilder.insert(strBuilder.length - decimals, separator)
        }
        while(strBuilder.substring(strBuilder.indexOf(separator)).length > 3 && strBuilder.endsWith("0")) {
            strBuilder.delete(strBuilder.length - 1, strBuilder.length)
        }

        if (strBuilder.toString().startsWith(separator))
            strBuilder.insert(0, "0")

        if (withGStroke) {
            strBuilder.insert(0, gStroke)
        }
        if (isNegative) {
            strBuilder.insert(0, "-")
        }

        return strBuilder.toString().removeSuffix(separator.toString())
    }

    fun getWholePart(stringValue: String): Long? {
        if (stringValue.isEmpty()) {
            return null
        }
        if (!checkGTUString(stringValue)) {
            return null
        }
        val decimalSeparatorIndex = stringValue.indexOf(separator)
        return if (decimalSeparatorIndex == -1) {
            stringValue.toLong()
        } else {
            stringValue.substring(0, decimalSeparatorIndex).toLong()
        }
    }

    fun toGTUValue(stringValue: String, token: Token?): BigDecimal? {
        var decimals = 6
        token?.let {
            it.tokenMetadata?.let { tokenMetadata ->
                decimals = tokenMetadata.decimals
            }
        }
        return toGTUValue(stringValue, decimals)
    }

    fun toGTUValue(stringValue: String, decimals: Int = 6): BigDecimal? {
        var str = stringValue.replace("Ͼ", "")
        if (str.isEmpty()) {
            return null
        }
        if (!checkGTUString(str)) {
            return null
        }
        val decimalSeparatorIndex = str.indexOf(separator)
        // Ensure that there are 6 decimals
        if (decimalSeparatorIndex == -1) {
            str = "$str${separator}000000"
        } else {
            val missingZeros = 6 - ((str.length - 1) - decimalSeparatorIndex)
            for (i in 1..missingZeros) {
                str += "0"
            }
        }
        // Remove the separator to get the value (because there are four decimals)
        val noDecimalSeparatorString = str.replace("$separator", "")
        return noDecimalSeparatorString.toBigDecimal()
    }

    private fun checkGTUString(stringValue: String): Boolean {
        return patternGTU.matcher(stringValue).matches()
    }
}
