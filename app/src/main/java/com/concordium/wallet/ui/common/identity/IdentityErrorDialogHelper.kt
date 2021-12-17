package com.concordium.wallet.ui.common.identity

import android.app.Activity
import android.content.*
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.ui.RequestCodes
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityErrorData
import com.concordium.wallet.uicore.dialog.CustomDialogFragment
import com.concordium.wallet.uicore.dialog.Dialogs
import com.google.android.material.snackbar.Snackbar
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
            if(IdentityErrorDialogHelper.canOpenSupportEmail(activity)){
                dialogs.showPositiveSupportDialog(
                    activity,
                    RequestCodes.REQUEST_IDENTITY_ERROR_DIALOG,
                    title,
                    activity.getString(R.string.dialog_popup_support_with_email_client_text, identityErrorData.identity.identityProvider.ipInfo.ipDescription.name),
                    positive,
                    R.string.dialog_support,
                    R.string.dialog_cancel,
                    hash(identityErrorData.identity.codeUri),
                    identityErrorData.identity.identityProvider.metadata.getSupportWithDefault()
                )
            }
            else{
                dialogs.showPositiveSupportDialog(
                    activity,
                    RequestCodes.REQUEST_IDENTITY_ERROR_DIALOG,
                    title,
                    activity.getString(R.string.dialog_popup_support_without_email_client_text, identityErrorData.identity.identityProvider.ipInfo.ipDescription.name, identityErrorData.identity.identityProvider.metadata.getSupportWithDefault()),
                    positive,
                    R.string.dialog_copy,
                    R.string.dialog_cancel,
                    hash(identityErrorData.identity.codeUri),
                    identityErrorData.identity.identityProvider.metadata.getSupportWithDefault()
                )
            }
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

    /**
     * Returns true if intent can be resolved
     * @param testOnly if set to true we to not resolve and start activity, we only test
     * @return true if intent can be resolved
     */
    fun openSupportEmail(context: Context, resources: Resources, identityEmail: String, uriSession: String, testOnly: Boolean = false): Boolean {
        val uriText = "mailto:" +identityEmail+
                "?cc=" + "idiss@concordium.software" +
                "&subject=" + Uri.encode(resources.getString(R.string.dialog_support_subject_reference, uriSession)) +
                "&body=" + Uri.encode(resources.getString(R.string.dialog_support_text, uriSession, BuildConfig.VERSION_NAME, Build.VERSION.RELEASE))
        val uri = Uri.parse(uriText)
        val sendIntent = Intent(Intent.ACTION_SENDTO)
        sendIntent.data = uri
        if (sendIntent.resolveActivity(context.packageManager) != null) {
            if(!testOnly){
                context.startActivity(Intent.createChooser(sendIntent, resources.getString(R.string.dialog_send_email)))
            }
            return true
        }
        else{
            return false
        }
    }

    fun openGenericSupportEmail(context: Context, resources: Resources, subject: String, text: String) {
        val uriText = "mailto: support@concordium.software"+
                "?subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(text)
        val uri = Uri.parse(uriText)
        val sendIntent = Intent(Intent.ACTION_SENDTO)
        sendIntent.data = uri
        context.startActivity(Intent.createChooser(sendIntent, resources.getString(R.string.dialog_send_email)))
    }

    /**
     * Convenience method
     */
    fun canOpenSupportEmail(context: Context): Boolean {
        if(BuildConfig.FORCE_NO_EMAIL_CLIENTS){
            return false
        }
        return openSupportEmail(context, context.resources, "","", true)
    }




    fun copyToClipboard(context: Context, title: String, content: String) {
        val clipboard: ClipboardManager = context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(title, content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.resources.getString(R.string.contact_issuance_hash_value_copied), Toast.LENGTH_SHORT).show()
    }


}