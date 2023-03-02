package com.concordium.wallet.data.util

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.preferences.SharedPreferencesKeys
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PreferencesMigrationShould {
    private lateinit var authPreferences : AuthPreferences
    lateinit var unencryptedPreferences : SharedPreferences

    private val context =
        ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp(){

        unencryptedPreferences = context.getSharedPreferences(SharedPreferencesKeys.PREF_FILE_AUTH, Context.MODE_PRIVATE)
        unencryptedPreferences.edit().putBoolean(AuthPreferences.PREFKEY_HAS_SETUP_USER, true).commit()
    }

    @Test
    fun encryptAndClearOldPreferences(){
        Assert.assertFalse(unencryptedPreferences.all.isEmpty())
        Assert.assertEquals(unencryptedPreferences.getBoolean(AuthPreferences.PREFKEY_HAS_SETUP_USER, false), true)
        authPreferences = AuthPreferences(context)
        Assert.assertTrue(unencryptedPreferences.all.isEmpty())
        Assert.assertTrue(authPreferences.getHasSetupUser())
    }
}