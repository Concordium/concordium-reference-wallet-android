package com.concordium.wallet.util

import com.concordium.wallet.App
import com.concordium.wallet.R

object ValidationUtil {

    fun validateName(str: String): Boolean {
        return str.all {
            it.isLetterOrDigit() || App.appContext.getString(R.string.valid_special_chars_for_names)
                .contains(it)
        } &&
                !str.toString().startsWith(" ") &&
                !str.toString().endsWith(" ")
    }
}