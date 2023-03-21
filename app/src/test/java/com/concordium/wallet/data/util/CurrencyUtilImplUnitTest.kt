package com.concordium.wallet.data.util
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.DecimalFormatSymbols

class CurrencyUtilImplUnitTest {

    private val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator

    private fun replaceDecimalSep(str: String): String {
        if (decimalSeparator != '.') {
            return str.replace('.', decimalSeparator)
        }
        return str
    }

    @Test
    fun formatGTU() {
        assertEquals(replaceDecimalSep("0.00"), CurrencyUtilImplMock.formatGTU(0))

        assertEquals(replaceDecimalSep("1.00"), CurrencyUtilImplMock.formatGTU(1000000))
        assertEquals(replaceDecimalSep("100.00"), CurrencyUtilImplMock.formatGTU(100000000))
        assertEquals(replaceDecimalSep("1.20"), CurrencyUtilImplMock.formatGTU(1200000))
        assertEquals(replaceDecimalSep("1.23"), CurrencyUtilImplMock.formatGTU(1230000))
        assertEquals(replaceDecimalSep("1.234"), CurrencyUtilImplMock.formatGTU(1234000))
        assertEquals(replaceDecimalSep("1.2345"), CurrencyUtilImplMock.formatGTU(1234500))
        assertEquals(replaceDecimalSep("123.4567"), CurrencyUtilImplMock.formatGTU(123456700))
        assertEquals(replaceDecimalSep("0.0001"), CurrencyUtilImplMock.formatGTU(100))
        assertEquals(replaceDecimalSep("0.0012"), CurrencyUtilImplMock.formatGTU(1200))
        assertEquals(replaceDecimalSep("0.01"), CurrencyUtilImplMock.formatGTU(10000))
        assertEquals(replaceDecimalSep("0.0123"), CurrencyUtilImplMock.formatGTU(12300))
        assertEquals(replaceDecimalSep("0.20"), CurrencyUtilImplMock.formatGTU(200000))
        assertEquals(replaceDecimalSep("0.23"), CurrencyUtilImplMock.formatGTU(230000))
        assertEquals(replaceDecimalSep("0.234"), CurrencyUtilImplMock.formatGTU(234000))
        assertEquals(replaceDecimalSep("0.2345"), CurrencyUtilImplMock.formatGTU(234500))

        assertEquals(replaceDecimalSep("-1.00"), CurrencyUtilImplMock.formatGTU(-1000000))
        assertEquals(replaceDecimalSep("-100.00"), CurrencyUtilImplMock.formatGTU(-100000000))
        assertEquals(replaceDecimalSep("-1.20"), CurrencyUtilImplMock.formatGTU(-1200000))
        assertEquals(replaceDecimalSep("-1.23"), CurrencyUtilImplMock.formatGTU(-1230000))
        assertEquals(replaceDecimalSep("-1.234"), CurrencyUtilImplMock.formatGTU(-1234000))
        assertEquals(replaceDecimalSep("-1.2345"), CurrencyUtilImplMock.formatGTU(-1234500))
        assertEquals(replaceDecimalSep("-123.4567"), CurrencyUtilImplMock.formatGTU(-123456700))
        assertEquals(replaceDecimalSep("-0.0001"), CurrencyUtilImplMock.formatGTU(-100))
        assertEquals(replaceDecimalSep("-0.0012"), CurrencyUtilImplMock.formatGTU(-1200))
        assertEquals(replaceDecimalSep("-0.01"), CurrencyUtilImplMock.formatGTU(-10000))
        assertEquals(replaceDecimalSep("-0.0123"), CurrencyUtilImplMock.formatGTU(-12300))
        assertEquals(replaceDecimalSep("-0.20"), CurrencyUtilImplMock.formatGTU(-200000))
        assertEquals(replaceDecimalSep("-0.23"), CurrencyUtilImplMock.formatGTU(-230000))
        assertEquals(replaceDecimalSep("-0.234"), CurrencyUtilImplMock.formatGTU(-234000))
        assertEquals(replaceDecimalSep("-0.2345"), CurrencyUtilImplMock.formatGTU(-234500))

        assertEquals(replaceDecimalSep("-100.234547"), CurrencyUtilImplMock.formatGTU(-100234547))
        assertEquals(replaceDecimalSep("-100.23454"), CurrencyUtilImplMock.formatGTU(-100234540))
        assertEquals(replaceDecimalSep("-100.2345"), CurrencyUtilImplMock.formatGTU(-100234500))
        assertEquals(replaceDecimalSep("-100.234"), CurrencyUtilImplMock.formatGTU(-100234000))
        assertEquals(replaceDecimalSep("-100.23"), CurrencyUtilImplMock.formatGTU(-100230000))
        assertEquals(replaceDecimalSep("-100.20"), CurrencyUtilImplMock.formatGTU(-100200000))
        assertEquals(replaceDecimalSep("-100.00"), CurrencyUtilImplMock.formatGTU(-100000000))

        assertEquals("c${replaceDecimalSep("0.00")}", CurrencyUtilImplMock.formatGTU(0, withGStroke = true))
        assertEquals("c${replaceDecimalSep("1.00")}", CurrencyUtilImplMock.formatGTU(1000000, withGStroke = true))
        assertEquals("-c${replaceDecimalSep("1.00")}", CurrencyUtilImplMock.formatGTU(-1000000, withGStroke = true))
    }

    @Test
    fun toGTUValue() {
        assertEquals(1000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("1.00")))
        assertEquals(1000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("1")))
        assertEquals(123456000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("123456")))
        assertEquals(1000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("1.0")))
        assertEquals(1000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("1.000")))
        assertEquals(1000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("1.0000")))
        assertEquals(1000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("1.000000")))
        assertEquals(null, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("1.00.00.00")))
        assertEquals(null, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("")))
        assertEquals(null, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("abc")))

        assertEquals(-1000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("-1.00")))
        assertEquals(-1000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("-1")))
        assertEquals(-123456000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("-123456")))
        assertEquals(-1000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("-1.0")))
        assertEquals(-1000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("-1.000")))
        assertEquals(-1000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("-1.0000")))
        assertEquals(-1000000L, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("-1.000000")))
        assertEquals(null, CurrencyUtilImplMock.toGTUValue(replaceDecimalSep("-1.00.00.00")))
    }
}
