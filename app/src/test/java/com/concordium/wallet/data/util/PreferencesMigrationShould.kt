package com.concordium.wallet.data.util

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.preferences.SharedPreferencesKeys
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


/**
 * Unit Tests for the migration from unencrypted to encrypted [SharedPreferences] performed in [Preferences]
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PreferencesMigrationShould {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun encryptAndClearOldPreferences(){
        val unencryptedPreferences = context.getSharedPreferences(SharedPreferencesKeys.PREF_FILE_AUTH, Context.MODE_PRIVATE)
        //Add values so that we can simulate update scenario
        unencryptedPreferences.edit()
            .putBoolean(AuthPreferences.PREFKEY_HAS_SETUP_USER, true)
            .putString(AuthPreferences.PREFKEY_BIOMETRIC_KEY, "test key")
            .commit()

        //Check to see that the values are saved
        Assert.assertFalse(unencryptedPreferences.all.isEmpty())
        Assert.assertTrue(unencryptedPreferences.getBoolean(AuthPreferences.PREFKEY_HAS_SETUP_USER, false))
        Assert.assertEquals(unencryptedPreferences.getString(AuthPreferences.PREFKEY_BIOMETRIC_KEY, ""), "test key")

        //Init the AuthPreferences
        val authPreferences = AuthPreferences(context)

        //Now the old preferences should be empty
        Assert.assertTrue(unencryptedPreferences.all.isEmpty())

        //Check the migration to the EncryptedSharedPreferences
        Assert.assertTrue(authPreferences.getHasSetupUser())
        Assert.assertEquals(authPreferences.getAuthKeyName(), "test key")
    }

    @Test
    fun useEncryptedSharedPreferencesForNewInstalls(){
        val authPreferences = AuthPreferences(context)

        authPreferences.setHasSetupUser(true)
        //Check if saved in EncryptedSharedPreferences
        Assert.assertTrue(authPreferences.getHasSetupUser())

        //Check to see if the unencrypted shared preferences are empty
        val unencryptedPreferences = context.getSharedPreferences(SharedPreferencesKeys.PREF_FILE_AUTH, Context.MODE_PRIVATE)
        Assert.assertTrue(unencryptedPreferences.all.isEmpty())
    }
}