package com.concordium.wallet.data.util

import com.concordium.wallet.util.toBigInteger
import org.junit.Assert.assertEquals
import org.junit.Test
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

        CurrencyUtil.setGstroke("Ͼ")

        assertEquals(replaceDecimalSep("0.00"), CurrencyUtil.formatGTU(0.toBigInteger()))

        assertEquals(replaceDecimalSep("1.00"), CurrencyUtil.formatGTU((1000000).toBigInteger()))
        assertEquals(replaceDecimalSep("100.00"), CurrencyUtil.formatGTU((100000000).toBigInteger()))
        assertEquals(replaceDecimalSep("1.20"), CurrencyUtil.formatGTU((1200000).toBigInteger()))
        assertEquals(replaceDecimalSep("1.23"), CurrencyUtil.formatGTU((1230000).toBigInteger()))
        assertEquals(replaceDecimalSep("1.234"), CurrencyUtil.formatGTU((1234000).toBigInteger()))
        assertEquals(replaceDecimalSep("1.2345"), CurrencyUtil.formatGTU((1234500).toBigInteger()))
        assertEquals(replaceDecimalSep("123.4567"), CurrencyUtil.formatGTU((123456700).toBigInteger()))
        assertEquals(replaceDecimalSep("0.0001"), CurrencyUtil.formatGTU((100).toBigInteger()))
        assertEquals(replaceDecimalSep("0.0012"), CurrencyUtil.formatGTU((1200).toBigInteger()))
        assertEquals(replaceDecimalSep("0.01"), CurrencyUtil.formatGTU((10000).toBigInteger()))
        assertEquals(replaceDecimalSep("0.0123"), CurrencyUtil.formatGTU((12300).toBigInteger()))
        assertEquals(replaceDecimalSep("0.20"), CurrencyUtil.formatGTU((200000).toBigInteger()))
        assertEquals(replaceDecimalSep("0.23"), CurrencyUtil.formatGTU((230000).toBigInteger()))
        assertEquals(replaceDecimalSep("0.234"), CurrencyUtil.formatGTU((234000).toBigInteger()))
        assertEquals(replaceDecimalSep("0.2345"), CurrencyUtil.formatGTU((234500).toBigInteger()))
        assertEquals(replaceDecimalSep("115792089237316195423570985008687907853269984665640564039457584007913129.639935"), CurrencyUtil.formatGTU("115792089237316195423570985008687907853269984665640564039457584007913129639935".toBigInteger()))
        assertEquals(replaceDecimalSep("115792089237316195423570985008687907853269984665640564039457.584007913129639935"), CurrencyUtil.formatGTU("115792089237316195423570985008687907853269984665640564039457584007913129639935".toBigInteger(), decimals = 18))

        assertEquals(replaceDecimalSep("-1.00"), CurrencyUtil.formatGTU((-1000000).toBigInteger()))
        assertEquals(replaceDecimalSep("-100.00"), CurrencyUtil.formatGTU((-100000000).toBigInteger()))
        assertEquals(replaceDecimalSep("-1.20"), CurrencyUtil.formatGTU((-1200000).toBigInteger()))
        assertEquals(replaceDecimalSep("-1.23"), CurrencyUtil.formatGTU((-1230000).toBigInteger()))
        assertEquals(replaceDecimalSep("-1.234"), CurrencyUtil.formatGTU((-1234000).toBigInteger()))
        assertEquals(replaceDecimalSep("-1.2345"), CurrencyUtil.formatGTU((-1234500).toBigInteger()))
        assertEquals(replaceDecimalSep("-123.4567"), CurrencyUtil.formatGTU((-123456700).toBigInteger()))
        assertEquals(replaceDecimalSep("-0.0001"), CurrencyUtil.formatGTU((-100).toBigInteger()))
        assertEquals(replaceDecimalSep("-0.0012"), CurrencyUtil.formatGTU((-1200).toBigInteger()))
        assertEquals(replaceDecimalSep("-0.01"), CurrencyUtil.formatGTU((-10000).toBigInteger()))
        assertEquals(replaceDecimalSep("-0.0123"), CurrencyUtil.formatGTU((-12300).toBigInteger()))
        assertEquals(replaceDecimalSep("-0.20"), CurrencyUtil.formatGTU((-200000).toBigInteger()))
        assertEquals(replaceDecimalSep("-0.23"), CurrencyUtil.formatGTU((-230000).toBigInteger()))
        assertEquals(replaceDecimalSep("-0.234"), CurrencyUtil.formatGTU((-234000).toBigInteger()))
        assertEquals(replaceDecimalSep("-0.2345"), CurrencyUtil.formatGTU((-234500).toBigInteger()))

        assertEquals(replaceDecimalSep("-100.234547"), CurrencyUtil.formatGTU((-100234547).toBigInteger()))
        assertEquals(replaceDecimalSep("-100.23454"), CurrencyUtil.formatGTU((-100234540).toBigInteger()))
        assertEquals(replaceDecimalSep("-100.2345"), CurrencyUtil.formatGTU((-100234500).toBigInteger()))
        assertEquals(replaceDecimalSep("-100.234"), CurrencyUtil.formatGTU((-100234000).toBigInteger()))
        assertEquals(replaceDecimalSep("-100.23"), CurrencyUtil.formatGTU((-100230000).toBigInteger()))
        assertEquals(replaceDecimalSep("-100.20"), CurrencyUtil.formatGTU((-100200000).toBigInteger()))
        assertEquals(replaceDecimalSep("-100.00"), CurrencyUtil.formatGTU((-100000000).toBigInteger()))

        assertEquals("Ͼ${replaceDecimalSep("0.00")}", CurrencyUtil.formatGTU((0).toBigInteger(), withGStroke = true))
        assertEquals("Ͼ${replaceDecimalSep("1.00")}", CurrencyUtil.formatGTU((1000000).toBigInteger(), withGStroke = true))
        assertEquals(replaceDecimalSep("-Ͼ1.00"), CurrencyUtil.formatGTU((-1000000).toBigInteger(), withGStroke = true))
    }

    @Test
    fun toGTUValue() {
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.00"))?.longValueExact())
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1"))?.longValueExact())
        assertEquals(123456000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("123456"))?.longValueExact())
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.0"))?.longValueExact())
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.000"))?.longValueExact())
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.0000"))?.longValueExact())
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.000000"))?.longValueExact())
        assertEquals("115792089237316195423570985008687907853269984665640564039457584007913129639935".toBigInteger(), CurrencyUtil.toGTUValue(replaceDecimalSep("115792089237316195423570985008687907853269984665640564039457584007913129.639935")))
        assertEquals("115792089237316195423570985008687907853269984665640564039457584007913129639935".toBigInteger(), CurrencyUtil.toGTUValue(replaceDecimalSep("115792089237316195423570985008687907853269984665640564039457.584007913129639935"), decimals = 18))
        assertEquals(null, CurrencyUtil.toGTUValue(replaceDecimalSep("1.00.00.00"))?.longValueExact())
        assertEquals(null, CurrencyUtil.toGTUValue(replaceDecimalSep(""))?.longValueExact())
        assertEquals(null, CurrencyUtil.toGTUValue(replaceDecimalSep("abc"))?.longValueExact())

        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.00"))?.longValueExact())
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1"))?.longValueExact())
        assertEquals(-123456000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-123456"))?.longValueExact())
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.0"))?.longValueExact())
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.000"))?.longValueExact())
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.0000"))?.longValueExact())
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.000000"))?.longValueExact())
        assertEquals(null, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.00.00.00"))?.longValueExact())

    }
}
