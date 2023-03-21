package com.concordium.wallet.data.util

import com.concordium.wallet.data.model.Token
import java.text.DecimalFormatSymbols
import java.util.regex.Pattern

object CurrencyUtilImplMock : CurrencyUtil {
    private val separator: Char = DecimalFormatSymbols.getInstance().decimalSeparator
    private val patternGTU: Pattern = Pattern.compile("^-?[0-9]*[${separator}]?[0-9]{0,6}\$")
    var gStroke = getGstroke()

    override fun formatGTU(value: String, withGStroke: Boolean, decimals: Int): String {
        val valueLong = value.toLong()
        return formatGTU(valueLong, withGStroke, decimals)
    }

    override fun formatGTU(value: Long, token: Token?): String {
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

    override fun formatGTU(value: Long, withGStroke: Boolean, decimals: Int): String {
        if (decimals <= 0) {
            if (withGStroke)
                return gStroke + value.toString()
            return value.toString()
        }

        val isNegative = value < 0
        val str = if (isNegative) value.toString().replace("-", "") else value.toString()
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

    override fun getWholePart(stringValue: String): Long? {
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

    override fun toGTUValue(stringValue: String, token: Token?): Long? {
        var decimals = 6
        token?.let {
            it.tokenMetadata?.let { tokenMetadata ->
                decimals = tokenMetadata.decimals
            }
        }
        return toGTUValue(stringValue, decimals)
    }

    override fun toGTUValue(stringValue: String, decimals: Int): Long? {
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
        return noDecimalSeparatorString.toLongOrNull()
    }

    private fun checkGTUString(stringValue: String): Boolean {
        return patternGTU.matcher(stringValue).matches()
    }

    override fun getGstroke(): String = "c"
}
