package com.concordium.wallet.data.preferences

import android.content.Context
import android.content.SharedPreferences

open class Preferences {

    protected lateinit var sharedPreferences: SharedPreferences

    protected val editor: android.content.SharedPreferences.Editor
        get() = sharedPreferences.edit()

    private val changeListeners = HashMap<Listener, String>()

    constructor() {}

    interface Listener {
        fun onChange()
    }

    constructor(context: Context, preferenceName: String, preferenceMode: Int) {
        sharedPreferences = context.getSharedPreferences(preferenceName, preferenceMode)
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

    public fun addListener(key: String, listener: Listener){
        changeListeners.put(listener, key)
    }

    public fun removeListener(listener: Listener){
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

    protected fun setLong(key: String, value: Long) {
        val editor = editor
        editor.remove(key)
        editor.putLong(key, value)
        editor.commit()
        triggerChangeEvent(key)
    }

    protected fun getLong(key: String, def: Long): Long {
        return sharedPreferences.getLong(key, def)
    }

    protected fun getLong(key: String): Long {
        return sharedPreferences.getLong(key, 0)
    }




}
