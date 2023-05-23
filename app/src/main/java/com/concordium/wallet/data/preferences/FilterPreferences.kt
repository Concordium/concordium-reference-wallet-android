package com.concordium.wallet.data.preferences

import android.content.Context

class FilterPreferences(val context: Context) :
    Preferences(context, SharedPreferencesKeys.PREF_FILE_FILTER.key, Context.MODE_PRIVATE) {

    companion object {
        val PREFKEY_FILTER_SHOW_REWARDS = "PREFKEY_FILTER_SHOW_REWARDS"
        val PREFKEY_FILTER_SHOW_FINALIZATION_REWARDS = "PREFKEY_FILTER_SHOW_FINALIZATION_REWARDS"
    }

    fun setHasShowRewards(id: Int, value: Boolean) {
        setBoolean(PREFKEY_FILTER_SHOW_REWARDS + id, value)
    }

    fun getHasShowRewards(id: Int): Boolean {
        return getBoolean(PREFKEY_FILTER_SHOW_REWARDS + id, true)
    }

    fun setHasShowFinalizationRewards(id: Int, value: Boolean) {
        setBoolean(PREFKEY_FILTER_SHOW_FINALIZATION_REWARDS + id, value)
    }

    fun getHasShowFinalizationRewards(id: Int): Boolean {
        return getBoolean(PREFKEY_FILTER_SHOW_FINALIZATION_REWARDS + id, true)
    }

}
