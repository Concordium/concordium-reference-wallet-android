package com.concordium.wallet.data.util

import com.concordium.wallet.util.equalsArithmetically
import com.concordium.wallet.util.toBigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
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

        assertEquals(replaceDecimalSep("0.00"), CurrencyUtil.formatGTU(BigDecimal(0)))

        assertEquals(replaceDecimalSep("1.00"), CurrencyUtil.formatGTU(BigDecimal(1000000)))
        assertEquals(replaceDecimalSep("100.00"), CurrencyUtil.formatGTU(BigDecimal(100000000)))
        assertEquals(replaceDecimalSep("1.20"), CurrencyUtil.formatGTU(BigDecimal(1200000)))
        assertEquals(replaceDecimalSep("1.23"), CurrencyUtil.formatGTU(BigDecimal(1230000)))
        assertEquals(replaceDecimalSep("1.234"), CurrencyUtil.formatGTU(BigDecimal(1234000)))
        assertEquals(replaceDecimalSep("1.2345"), CurrencyUtil.formatGTU(BigDecimal(1234500)))
        assertEquals(replaceDecimalSep("123.4567"), CurrencyUtil.formatGTU(BigDecimal(123456700)))
        assertEquals(replaceDecimalSep("0.0001"), CurrencyUtil.formatGTU(BigDecimal(100)))
        assertEquals(replaceDecimalSep("0.0012"), CurrencyUtil.formatGTU(BigDecimal(1200)))
        assertEquals(replaceDecimalSep("0.01"), CurrencyUtil.formatGTU(BigDecimal(10000)))
        assertEquals(replaceDecimalSep("0.0123"), CurrencyUtil.formatGTU(BigDecimal(12300)))
        assertEquals(replaceDecimalSep("0.20"), CurrencyUtil.formatGTU(BigDecimal(200000)))
        assertEquals(replaceDecimalSep("0.23"), CurrencyUtil.formatGTU(BigDecimal(230000)))
        assertEquals(replaceDecimalSep("0.234"), CurrencyUtil.formatGTU(BigDecimal(234000)))
        assertEquals(replaceDecimalSep("0.2345"), CurrencyUtil.formatGTU(BigDecimal(234500)))
        assertEquals(replaceDecimalSep("115792089237316195423570985008687907853269984665640564039457584007913129.639935"), CurrencyUtil.formatGTU("115792089237316195423570985008687907853269984665640564039457584007913129639935".toBigDecimal()))
        assertEquals(replaceDecimalSep("115792089237316195423570985008687907853269984665640564039457.584007913129639935"), CurrencyUtil.formatGTU("115792089237316195423570985008687907853269984665640564039457584007913129639935".toBigDecimal(), decimals = 18))

        assertEquals(replaceDecimalSep("-1.00"), CurrencyUtil.formatGTU(BigDecimal(-1000000)))
        assertEquals(replaceDecimalSep("-100.00"), CurrencyUtil.formatGTU(BigDecimal(-100000000)))
        assertEquals(replaceDecimalSep("-1.20"), CurrencyUtil.formatGTU(BigDecimal(-1200000)))
        assertEquals(replaceDecimalSep("-1.23"), CurrencyUtil.formatGTU(BigDecimal(-1230000)))
        assertEquals(replaceDecimalSep("-1.234"), CurrencyUtil.formatGTU(BigDecimal(-1234000)))
        assertEquals(replaceDecimalSep("-1.2345"), CurrencyUtil.formatGTU(BigDecimal(-1234500)))
        assertEquals(replaceDecimalSep("-123.4567"), CurrencyUtil.formatGTU(BigDecimal(-123456700)))
        assertEquals(replaceDecimalSep("-0.0001"), CurrencyUtil.formatGTU(BigDecimal(-100)))
        assertEquals(replaceDecimalSep("-0.0012"), CurrencyUtil.formatGTU(BigDecimal(-1200)))
        assertEquals(replaceDecimalSep("-0.01"), CurrencyUtil.formatGTU(BigDecimal(-10000)))
        assertEquals(replaceDecimalSep("-0.0123"), CurrencyUtil.formatGTU(BigDecimal(-12300)))
        assertEquals(replaceDecimalSep("-0.20"), CurrencyUtil.formatGTU(BigDecimal(-200000)))
        assertEquals(replaceDecimalSep("-0.23"), CurrencyUtil.formatGTU(BigDecimal(-230000)))
        assertEquals(replaceDecimalSep("-0.234"), CurrencyUtil.formatGTU(BigDecimal(-234000)))
        assertEquals(replaceDecimalSep("-0.2345"), CurrencyUtil.formatGTU(BigDecimal(-234500)))

        assertEquals(replaceDecimalSep("-100.234547"), CurrencyUtil.formatGTU(BigDecimal(-100234547)))
        assertEquals(replaceDecimalSep("-100.23454"), CurrencyUtil.formatGTU(BigDecimal(-100234540)))
        assertEquals(replaceDecimalSep("-100.2345"), CurrencyUtil.formatGTU(BigDecimal(-100234500)))
        assertEquals(replaceDecimalSep("-100.234"), CurrencyUtil.formatGTU(BigDecimal(-100234000)))
        assertEquals(replaceDecimalSep("-100.23"), CurrencyUtil.formatGTU(BigDecimal(-100230000)))
        assertEquals(replaceDecimalSep("-100.20"), CurrencyUtil.formatGTU(BigDecimal(-100200000)))
        assertEquals(replaceDecimalSep("-100.00"), CurrencyUtil.formatGTU(BigDecimal(-100000000)))

        assertEquals("Ͼ${replaceDecimalSep("0.00")}", CurrencyUtil.formatGTU(BigDecimal(0), withGStroke = true))
        assertEquals("Ͼ${replaceDecimalSep("1.00")}", CurrencyUtil.formatGTU(BigDecimal(1000000), withGStroke = true))
        assertEquals(replaceDecimalSep("-Ͼ1.00"), CurrencyUtil.formatGTU(BigDecimal(-1000000), withGStroke = true))
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
        assertTrue("115792089237316195423570985008687907853269984665640564039457584007913129639935".toBigDecimal().equalsArithmetically(CurrencyUtil.toGTUValue(replaceDecimalSep("115792089237316195423570985008687907853269984665640564039457584007913129.639935"))))
        assertTrue("115792089237316195423570985008687907853269984665640564039457584007913129639935".toBigDecimal().equalsArithmetically(CurrencyUtil.toGTUValue(replaceDecimalSep("115792089237316195423570985008687907853269984665640564039457.584007913129639935"), decimals = 18)))
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
