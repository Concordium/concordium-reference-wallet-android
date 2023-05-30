package com.concordium.wallet

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Suppress("KotlinConstantConditions")
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedSuffix = when (BuildConfig.FLAVOR) {
            "prodTestNet" -> "testnet"
            "prodMainNet" -> "mainnet"
            else -> BuildConfig.FLAVOR.lowercase(Locale.ENGLISH)
        }
        assertEquals("software.concordium.mobilewallet.seedphrase.$expectedSuffix", appContext.packageName)
    }
}
