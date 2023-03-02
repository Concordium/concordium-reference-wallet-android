package com.concordium.wallet.data.preferences

class SharedPreferencesKeys {

    companion object {
        const val PREF_FILE_AUTH = "PREF_FILE_AUTH"
        const val PREF_FILE_FILTER = "PREF_FILE_FILTER"
        const val KEY_IDENTITY_CREATION_DATA = "KEY_IDENTITY_CREATION_DATA"
        const val PREF_SEND_FUNDS = "PREF_SEND_FUNDS"
        val PREF_ALL = arrayListOf(PREF_FILE_AUTH, PREF_FILE_FILTER, KEY_IDENTITY_CREATION_DATA, PREF_SEND_FUNDS)
    }
}