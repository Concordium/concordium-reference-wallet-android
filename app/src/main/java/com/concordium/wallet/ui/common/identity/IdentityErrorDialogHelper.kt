package com.concordium.wallet.ui.common.identity

import androidx.appcompat.app.AppCompatActivity
import com.concordium.wallet.R
import com.concordium.wallet.ui.RequestCodes
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityErrorData
import com.concordium.wallet.uicore.dialog.Dialogs

object IdentityErrorDialogHelper {

    fun showIdentityError(activity: AppCompatActivity, dialogs: Dialogs, identityErrorData: IdentityErrorData) {
        val title = R.string.dialog_initial_account_error_title
        val text =
            if (identityErrorData.isFirstIdentity) R.string.dialog_initial_account_error_text_first else R.string.dialog_initial_account_error_text
        val positive = R.string.dialog_initial_account_error_positive
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