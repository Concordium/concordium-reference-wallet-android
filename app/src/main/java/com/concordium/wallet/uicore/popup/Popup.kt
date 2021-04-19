package com.concordium.wallet.uicore.popup

import android.view.View
import com.google.android.material.snackbar.Snackbar

class Popup {

    fun showSnackbar(view: View, stringId: Int) {
        Snackbar.make(view, stringId, Snackbar.LENGTH_LONG).show()
    }

    fun showSnackbar(view: View, text: String) {
        Snackbar.make(view, text, Snackbar.LENGTH_LONG).show()
    }


}
