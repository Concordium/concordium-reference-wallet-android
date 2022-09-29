package com.concordium.wallet.ui.common.delegates

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import com.concordium.wallet.R

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
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=software.concordium.mobilewallet.seedphrase.mainnet")))
        }
        builder.setNegativeButton(activity.getString(R.string.dialog_cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
}
