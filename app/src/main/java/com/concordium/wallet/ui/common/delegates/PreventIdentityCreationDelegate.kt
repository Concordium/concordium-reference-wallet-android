package com.concordium.wallet.ui.common.delegates

import android.app.Activity
import android.app.AlertDialog
import com.concordium.wallet.R
import com.concordium.wallet.util.CryptoX

interface PreventIdentityCreationDelegate {
    fun preventNewId(activity: Activity)
}

class PreventIdentityCreationDelegateImpl : PreventIdentityCreationDelegate {
    override fun preventNewId(activity: Activity) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.identities_overview_not_supported_title)
        builder.setMessage(R.string.identities_overview_not_supported_message)
        builder.setPositiveButton(activity.getString(R.string.identities_overview_not_supported_download)) { dialog, _ ->
            dialog.dismiss()
            CryptoX.openMarket(activity)
        }
        builder.setNegativeButton(activity.getString(R.string.identities_overview_not_supported_okay)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
}
