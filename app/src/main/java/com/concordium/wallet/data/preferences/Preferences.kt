package com.concordium.wallet.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.concordium.wallet.util.Log


open class Preferences {

    private var sharedPreferences: SharedPreferences

    private val editor: SharedPreferences.Editor
        get() = sharedPreferences.edit()

    private val changeListeners = HashMap<Listener, String>()

    interface Listener {
        fun onChange()
    }
    companion object{
        const val SHARED_PREFERENCES_ARE_ENCRYPTED ="encrypted_shared_preference"
    }

    constructor(context: Context, preferenceName: String, preferenceMode: Int) {
        val authPreferences = initializeEncryptedSharedPreferences(context, SharedPreferencesKeys.PREF_FILE_AUTH)
        if(authPreferences.getBoolean(SHARED_PREFERENCES_ARE_ENCRYPTED, false)){
            sharedPreferences = initializeEncryptedSharedPreferences(context, preferenceName)
        }else{
            sharedPreferences =
                if(migratePreferencesSuccess(context, preferenceName, preferenceMode, authPreferences)){
                    initializeEncryptedSharedPreferences(context, preferenceName)
                }else{
                    getSharedPreferences(context, preferenceName, preferenceMode)
                }
        }
    }

    /**
     * Returns the required SharedPreferences
     * @return [SharedPreferences] *if* unencrypted data is still present
     *
     * *else* an instance of encrypted [SharedPreferences]
     */
    private fun getSharedPreferences(
        context: Context,
        preferenceName: String,
        preferenceMode: Int
    ): SharedPreferences {
        val unEncryptedSharedPreferences = context.getSharedPreferences(preferenceName, preferenceMode)

        return if(unEncryptedSharedPreferences.all.isEmpty()) {
            initializeEncryptedSharedPreferences(context, preferenceName)
        }else{
            unEncryptedSharedPreferences
        }
    }

    /**
     * Loops through all of the [SharedPreferences] and attempts to save them to [EncryptedSharedPreferences]
     * @return *true* if the [SharedPreferences] don't need migrating
     *
     * *true* if [SharedPreferences] is successfully migrated and the old data cleared
     *
     * *false* if the migrations fail or is not cleared successfully
     */
    private fun migratePreferencesSuccess(
        mContext: Context,
        preferenceName: String,
        preferenceMode: Int,
        authPreferences: SharedPreferences): Boolean{
        var allPreferencesAreMigrated = true
        var continueWithEncryptedSharedPreference = true

        for(prefName in SharedPreferencesKeys.PREF_ALL){
            if(!migrateSinglePreferencesIfNeededOrContinue(mContext, prefName, preferenceMode)){
                allPreferencesAreMigrated = false

                if(prefName == preferenceName) {
                    continueWithEncryptedSharedPreference = false
                }
            }
        }
        if(allPreferencesAreMigrated){
            authPreferences.edit().putBoolean(SHARED_PREFERENCES_ARE_ENCRYPTED, true).commit()
        }
        return  continueWithEncryptedSharedPreference
    }

    /**
     * Migrate single [SharedPreferences] to [EncryptedSharedPreferences]
     * @return *true* if the unencrypted preference is empty
     *
     * *true* if the preference is successfully migrated and cleared
     *
     * *false* if the migration fails
     *
     * *false* if the old preference in not cleared
     */
    private fun migrateSinglePreferencesIfNeededOrContinue(
        mContext: Context,
        preferenceName: String,
        preferenceMode: Int
    ): Boolean {
        val unencryptedPreference = mContext.getSharedPreferences(preferenceName, preferenceMode)
        var migrationIsSuccessful = true
        if(unencryptedPreference.all.isNotEmpty()){
            val encryptedSharedPreferences = initializeEncryptedSharedPreferences(mContext, preferenceName)

            for (entry in unencryptedPreference.all.entries) {
                val key = entry.key
                val value: Any? = entry.value
                if(!encryptedSharedPreferences.set(key, value)){
                    migrationIsSuccessful = false
                }
            }

            if(migrationIsSuccessful){
                if(!unencryptedPreference.edit().clear().commit()){
                    migrationIsSuccessful = false
                }
            }
        }
        return migrationIsSuccessful
    }

    fun SharedPreferences.set(key: String, value: Any?): Boolean {
        return when (value) {
            is String? -> edit { it.putString(key, value) }
            is Int -> edit { it.putInt(key, value.toInt()) }
            is Boolean -> edit { it.putBoolean(key, value) }
            is Float -> edit { it.putFloat(key, value.toFloat()) }
            is Long -> edit { it.putLong(key, value.toLong()) }
            else -> {
                Log.e("Unsupported Type: $value")
                false
            }
        }
    }

    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit): Boolean {
        val editor = this.edit()
        operation(editor)
        return editor.commit()
    }

    private fun initializeEncryptedSharedPreferences(
        context: Context,
        preferenceName: String
    ): SharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val encryptedPreferenceName = preferenceName +"_ENCRYPTED"
        return EncryptedSharedPreferences.create(
            context,
            encryptedPreferenceName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // registerOnSharedPreferenceChangeListener doesn't work
    fun triggerChangeEvent(key: String){
        for ((listener, value) in changeListeners) {
            if(value == key){
                listener.onChange()
            }
        }
    }

    fun clearAll() {
        val editor = editor
        editor.clear()
        editor.commit()
    }

    fun removeListener(listener: Listener){
        changeListeners.remove(listener)
    }

    protected fun setString(key: String, value: String?) {
        val editor = editor
        if (value == null) {
            editor.remove(key)
        } else {
            editor.putString(key, value)
        }
        editor.commit()
        triggerChangeEvent(key)
    }

    protected fun setStringWithResult(key: String, value: String?): Boolean {
        val editor = editor
        if (value == null) {
            editor.remove(key)
        } else {
            editor.putString(key, value)
        }
       return editor.commit()
    }

    protected fun getString(key: String, def: String): String {
        val result = sharedPreferences.getString(key, def)
        result ?: return def
        return result
    }

    protected fun getString(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    protected fun setBoolean(key: String, value: Boolean) {
        val editor = editor
        editor.remove(key)
        editor.putBoolean(key, value)
        editor.commit()
        triggerChangeEvent(key)
    }

    protected fun getBoolean(key: String, def: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, def)
    }

    protected fun getBoolean(key: String): Boolean {
        return sharedPreferences.getBoolean(key, false)
    }

    protected fun setInt(key: String, value: Int) {
        val editor = editor
        editor.remove(key)
        editor.putInt(key, value)
        editor.commit()
        triggerChangeEvent(key)
    }

    protected fun getInt(key: String, def: Int): Int {
        return sharedPreferences.getInt(key, def)
    }

    protected fun getInt(key: String): Int {
        return sharedPreferences.getInt(key, 0)
    }
}
