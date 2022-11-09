package com.concordium.wallet.ui.common.delegates

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import com.concordium.wallet.R

interface PreventAccountCreationDelegate {
    fun preventNewAccount(activity: Activity)
}

class PreventAccountCreationDelegateImpl : PreventAccountCreationDelegate {
    override fun preventNewAccount(activity: Activity) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.accounts_overview_not_supported_title)
        builder.setMessage(R.string.accounts_overview_not_supported_message)
        builder.setPositiveButton(activity.getString(R.string.accounts_overview_not_supported_download)) { dialog, _ ->
            dialog.dismiss()
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=software.concordium.mobilewallet.seedphrase.mainnet")))
        }
        builder.setNegativeButton(activity.getString(R.string.accounts_overview_not_supported_okay)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
}
