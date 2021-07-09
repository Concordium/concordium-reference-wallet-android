package com.concordium.wallet.ui.common.identity

import android.content.DialogInterface
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.ui.RequestCodes
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityErrorData
import com.concordium.wallet.uicore.dialog.CustomDialogFragment
import com.concordium.wallet.uicore.dialog.Dialogs
import java.security.MessageDigest

object IdentityErrorDialogHelper {

    fun hash(txt: String): String {
        val bytes = txt.toString().toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    fun showIdentityError(activity: AppCompatActivity, dialogs: Dialogs, identityErrorData: IdentityErrorData) {
        val title = R.string.dialog_initial_account_error_title
        val text =
            if (identityErrorData.isFirstIdentity) R.string.dialog_initial_account_error_text_first else R.string.dialog_initial_account_error_text
        val positive = R.string.dialog_initial_account_error_positive


        if(!TextUtils.isEmpty(identityErrorData.identity.codeUri)){

            dialogs.showPositiveSupportDialog(
                    activity,
                    RequestCodes.REQUEST_IDENTITY_ERROR_DIALOG,
                    title,
                    R.string.dialog_popup_support_text,
                    positive,
                    R.string.dialog_support,
                    hash(identityErrorData.identity.codeUri)
            )

        }
        else{
            if (identityErrorData.isFirstIdentity) {
                dialogs.showOkDialog(
                        activity,
                        RequestCodes.REQUEST_IDENTITY_ERROR_DIALOG,
                        title,
                        text,
                        positive
                )
            } else {
                dialogs.showPositiveNegativeDialog(
                        activity,
                        RequestCodes.REQUEST_IDENTITY_ERROR_DIALOG,
                        title,
                        text,
                        positive,
                        R.string.dialog_cancel
                )
            }
        }


    }
}