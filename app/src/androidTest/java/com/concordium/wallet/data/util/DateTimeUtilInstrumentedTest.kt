package com.concordium.wallet.data.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*


@RunWith(AndroidJUnit4::class)
class DateTimeUtilInstrumentedTest {

    companion object {

        lateinit var locale: Locale

        @BeforeClass
        @JvmStatic
        fun setup() {
            locale = Locale.getDefault()
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            Locale.setDefault(locale)
        }
    }

    private fun setLocale(language: String, country: String) {
        // This does not work for instrumented tests (it does for unit tests)
        val locale = Locale(language, country)
        // Update locale for date formatters
        Locale.setDefault(locale)
    }

    @Test
    fun testFormatting() {

        when (locale.country) {
            "US" -> {
                Log.d("Format test: US")
                // English
                //setLocale("en", "EN")
                assertEquals(
                    "May 1, 2021",
                    DateTimeUtil.formatDateAsLocalMedium(DateTimeUtil.parseLongDate("20210501"))
                )

                assertEquals("Jan 1, 2021", DateTimeUtil.convertLongDate("20210101"))
                assertEquals("May 1, 2021", DateTimeUtil.convertLongDate("20210501"))

                assertEquals("*******", DateTimeUtil.convertLongDate("*******")) // leave time as is if unparsable

                assertEquals("Jan 2021", DateTimeUtil.convertShortDate("202101"))
                assertEquals("May 2021", DateTimeUtil.convertShortDate("202105"))

                assertEquals("*******", DateTimeUtil.convertShortDate("*******")) // leave time as is if unparsable

            }
            "DK" -> {
                Log.d("Format test: DK")
                // Danish
                //setLocale("da", "DK")
                assertEquals(
                    "1. maj 2021",
                    DateTimeUtil.formatDateAsLocalMedium(DateTimeUtil.parseLongDate("20210501"))
                )

                assertEquals("1. jan. 2021", DateTimeUtil.convertLongDate("20210101"))
                assertEquals("1. maj 2021", DateTimeUtil.convertLongDate("20210501"))

                assertEquals("*******", DateTimeUtil.convertLongDate("*******")) // leave time as is if unparsable

                assertEquals("jan. 2021", DateTimeUtil.convertShortDate("202101"))
                assertEquals("maj 2021", DateTimeUtil.convertShortDate("202105"))

                assertEquals("*******", DateTimeUtil.convertShortDate("*******")) // leave time as is if unparsable

            }
            else -> {
                Log.d("Format test: No matching language/country")
            }
        }
    }
}
