package com.concordium.wallet.data.util

import org.junit.Test

import org.junit.Assert.*
import java.text.DecimalFormatSymbols

class CurrencyUtilUnitTest {

    private val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator

    private fun replaceDecimalSep(str: String): String {
        if (decimalSeparator != '.') {
            return str.replace('.', decimalSeparator)
        }
        return str
    }

    @Test
    fun formatGTU() {

        assertEquals(replaceDecimalSep("0.00"), CurrencyUtil.formatGTU(0))

        assertEquals(replaceDecimalSep("1.00"), CurrencyUtil.formatGTU(1000000))
        assertEquals(replaceDecimalSep("100.00"), CurrencyUtil.formatGTU(100000000))
        assertEquals(replaceDecimalSep("1.20"), CurrencyUtil.formatGTU(1200000))
        assertEquals(replaceDecimalSep("1.23"), CurrencyUtil.formatGTU(1230000))
        assertEquals(replaceDecimalSep("1.234"), CurrencyUtil.formatGTU(1234000))
        assertEquals(replaceDecimalSep("1.2345"), CurrencyUtil.formatGTU(1234500))
        assertEquals(replaceDecimalSep("123.4567"), CurrencyUtil.formatGTU(123456700))
        assertEquals(replaceDecimalSep("0.0001"), CurrencyUtil.formatGTU(100))
        assertEquals(replaceDecimalSep("0.0012"), CurrencyUtil.formatGTU(1200))
        assertEquals(replaceDecimalSep("0.01"), CurrencyUtil.formatGTU(10000))
        assertEquals(replaceDecimalSep("0.0123"), CurrencyUtil.formatGTU(12300))
        assertEquals(replaceDecimalSep("0.20"), CurrencyUtil.formatGTU(200000))
        assertEquals(replaceDecimalSep("0.23"), CurrencyUtil.formatGTU(230000))
        assertEquals(replaceDecimalSep("0.234"), CurrencyUtil.formatGTU(234000))
        assertEquals(replaceDecimalSep("0.2345"), CurrencyUtil.formatGTU(234500))

        assertEquals(replaceDecimalSep("-1.00"), CurrencyUtil.formatGTU(-1000000))
        assertEquals(replaceDecimalSep("-100.00"), CurrencyUtil.formatGTU(-100000000))
        assertEquals(replaceDecimalSep("-1.20"), CurrencyUtil.formatGTU(-1200000))
        assertEquals(replaceDecimalSep("-1.23"), CurrencyUtil.formatGTU(-1230000))
        assertEquals(replaceDecimalSep("-1.234"), CurrencyUtil.formatGTU(-1234000))
        assertEquals(replaceDecimalSep("-1.2345"), CurrencyUtil.formatGTU(-1234500))
        assertEquals(replaceDecimalSep("-123.4567"), CurrencyUtil.formatGTU(-123456700))
        assertEquals(replaceDecimalSep("-0.0001"), CurrencyUtil.formatGTU(-100))
        assertEquals(replaceDecimalSep("-0.0012"), CurrencyUtil.formatGTU(-1200))
        assertEquals(replaceDecimalSep("-0.01"), CurrencyUtil.formatGTU(-10000))
        assertEquals(replaceDecimalSep("-0.0123"), CurrencyUtil.formatGTU(-12300))
        assertEquals(replaceDecimalSep("-0.20"), CurrencyUtil.formatGTU(-200000))
        assertEquals(replaceDecimalSep("-0.23"), CurrencyUtil.formatGTU(-230000))
        assertEquals(replaceDecimalSep("-0.234"), CurrencyUtil.formatGTU(-234000))
        assertEquals(replaceDecimalSep("-0.2345"), CurrencyUtil.formatGTU(-234500))

        assertEquals(replaceDecimalSep("-100.234547"), CurrencyUtil.formatGTU(-100234547))
        assertEquals(replaceDecimalSep("-100.23454"), CurrencyUtil.formatGTU(-100234540))
        assertEquals(replaceDecimalSep("-100.2345"), CurrencyUtil.formatGTU(-100234500))
        assertEquals(replaceDecimalSep("-100.234"), CurrencyUtil.formatGTU(-100234000))
        assertEquals(replaceDecimalSep("-100.23"), CurrencyUtil.formatGTU(-100230000))
        assertEquals(replaceDecimalSep("-100.20"), CurrencyUtil.formatGTU(-100200000))
        assertEquals(replaceDecimalSep("-100.00"), CurrencyUtil.formatGTU(-100000000))


        //For the following to be tested it needs to be in an instumented test, or an
        // alternative solution for the resource string is needed: let the CurrencyUtil use a
        // ResourcecProvider.getString (that is set on the AppCore) instead of having reference to context
        //assertEquals(replaceDecimalSep("0.00"), CurrencyUtil.formatGTU(0, withGStroke = true))
        //assertEquals(replaceDecimalSep("1.00"), CurrencyUtil.formatGTU(10000, withGStroke = true))
        //assertEquals(replaceDecimalSep("-1.00"), CurrencyUtil.formatGTU(-10000, withGStroke = true))

    }

    @Test
    fun toGTUValue() {
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.00")))
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1")))
        assertEquals(123456000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("123456")))
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.0")))
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.000")))
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.0000")))
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.000000")))
        assertEquals(null, CurrencyUtil.toGTUValue(replaceDecimalSep("1.00.00.00")))
        assertEquals(null, CurrencyUtil.toGTUValue(replaceDecimalSep("")))
        assertEquals(null, CurrencyUtil.toGTUValue(replaceDecimalSep("abc")))

        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.00")))
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1")))
        assertEquals(-123456000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-123456")))
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.0")))
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.000")))
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.0000")))
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.000000")))
        assertEquals(null, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.00.00.00")))

    }
}
