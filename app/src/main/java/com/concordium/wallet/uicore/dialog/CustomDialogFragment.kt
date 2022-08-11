package com.concordium.wallet.uicore.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.ui.common.identity.IdentityErrorDialogHelper
import com.concordium.wallet.ui.identity.identitycreate.IdentityCreateActivity

class CustomDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "CustomDialogFragmentTag"
        const val KEY_REQUEST_CODE = "key_request_code"
        const val KEY_TYPE = "key_type"
        const val KEY_TITLE = "key_title"
        const val KEY_MESSAGE = "key_message"
        const val KEY_POSITIVE = "key_positive"
        const val KEY_NEUTRAL = "key_neutral"
        const val KEY_NEGATIVE = "key_negative"
        const val KEY_SUPPORT = "key_support"
        const val KEY_SUPPORT_EMAIL = "key_support_email"

        //region Create/cancel dialogs
        //************************************************************

        private fun dismissCustomDialog(activity: AppCompatActivity) {
            val dialogFragment = activity.supportFragmentManager.findFragmentByTag(TAG)
            if (dialogFragment is DialogFragment) {
                dialogFragment.dismiss()
            }
        }

        private fun createCustomDialog(
            activity: AppCompatActivity,
            requestCode: Int,
            dialogType: EDialogType,
            title: String,
            message: String,
            positiveButton: String?,
            neutralButton: String?,
            negativeButton: String?,
            uriSession: String?,
            supportEmail: String?
        ): CustomDialogFragment {
            dismissCustomDialog(activity)

            val fragment = CustomDialogFragment()
            val args = Bundle()
            args.putInt(KEY_REQUEST_CODE, requestCode)
            args.putInt(KEY_TYPE, dialogType.ordinal)
            args.putString(KEY_TITLE, title)
            args.putString(KEY_MESSAGE, message)
            if (dialogType == EDialogType.PositiveSupport) {
                args.putString(KEY_POSITIVE, positiveButton)
                args.putString(KEY_NEUTRAL, neutralButton)
                args.putString(KEY_NEGATIVE, negativeButton)
                args.putString(KEY_SUPPORT, uriSession)
                args.putString(KEY_SUPPORT_EMAIL, supportEmail)
            }
            if (dialogType == EDialogType.PositiveNegative) {
                args.putString(KEY_POSITIVE, positiveButton)
                args.putString(KEY_NEGATIVE, negativeButton)
            }
            if (dialogType == EDialogType.OK && positiveButton != null) {
                args.putString(KEY_POSITIVE, positiveButton)
            }
            fragment.arguments = args
            return fragment
        }

        fun createOkDialog(
            activity: AppCompatActivity,
            requestCode: Int,
            title: String,
            message: String,
            positive: String?
        ): CustomDialogFragment {
            return createCustomDialog(
                activity,
                requestCode,
                EDialogType.OK,
                title,
                message,
                positive,
                null,
                null,
                    null,
                null

            )
        }

        fun createOkCancelDialog(
            activity: AppCompatActivity,
            requestCode: Int,
            title: String,
            message: String
        ): CustomDialogFragment {
            return createCustomDialog(
                activity,
                requestCode,
                EDialogType.OKCancel,
                title,
                message,
                null,
                null,
                null,
                null,
                null

            )
        }

        fun createYesNoDialog(
            activity: AppCompatActivity,
            requestCode: Int,
            title: String,
            message: String
        ): CustomDialogFragment {
            return createCustomDialog(
                activity,
                requestCode,
                EDialogType.YesNo,
                title,
                message,
                null,
                null,
                null,
                null,
                null

            )
        }

        fun createPositiveNegativeDialog(
            activity: AppCompatActivity,
            requestCode: Int,
            title: String,
            message: String,
            positiveButton: String,
            negativeButton: String,
            uriSession: String?,
            supportEmail: String?
        ): CustomDialogFragment {
            return createCustomDialog(
                activity,
                requestCode,
                EDialogType.PositiveNegative,
                title,
                message,
                positiveButton,
                null,
                negativeButton,
                    uriSession,
                supportEmail
            )
        }

        fun createPositiveSupportDialog(
                activity: AppCompatActivity,
                requestCode: Int,
                title: String,
                message: String,
                positiveButton: String,
                neutralButton: String,
                negativeButton: String,
                uriSession: String?,
                supportEmail: String?
        ): CustomDialogFragment {
            return createCustomDialog(
                    activity,
                    requestCode,
                    EDialogType.PositiveSupport,
                    title,
                    message,
                    positiveButton,
                    neutralButton,
                    negativeButton,
                uriSession,
                supportEmail
            )
        }

        //endregion
    }

    private var dialogFragmentListener: Dialogs.DialogFragmentListener? = null
    private var requestCode: Int? = 0

    enum class EDialogType {
        OK, OKCancel, YesNo, PositiveNegative, PositiveSupport
    }

    //region Lifecycle
    //************************************************************

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            dialogFragmentListener = context as Dialogs.DialogFragmentListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + "must implement DialogFragmentListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val resources = App.appContext.resources
        val args: Bundle = requireArguments()
        requestCode = args.getInt(KEY_REQUEST_CODE, 0)
        val type = EDialogType.values()[args.getInt(KEY_TYPE, 0)]
        val title = args.getString(KEY_TITLE, "")
        val message = args.getString(KEY_MESSAGE, "")
        var resPositive = args.getString(KEY_POSITIVE, resources.getString(R.string.dialog_ok))
        val resNeutral = args.getString(KEY_NEUTRAL, null)
        var resNegative = args.getString(KEY_NEGATIVE, resources.getString(R.string.dialog_cancel))
        val uriSession = args.getString(KEY_SUPPORT, null)
        val supportEmail = args.getString(KEY_SUPPORT_EMAIL, null)
        if (type == EDialogType.YesNo) {
            resPositive = resources.getString(R.string.dialog_yes)
            resNegative = resources.getString(R.string.dialog_no)
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(true)//This have to be set on dialog to have effect
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(resPositive) { _, _ ->
            dialogFragmentListener?.onDialogResult(requestCode!!, Dialogs.POSITIVE, Intent())
        }
        if (type != EDialogType.OK && type != EDialogType.PositiveSupport) {
            builder.setNegativeButton(resNegative) { _, _ ->
                dialogFragmentListener?.onDialogResult(
                    requestCode!!,
                    Dialogs.NEGATIVE,
                    Intent()
                )
            }
        }
        if (type == EDialogType.PositiveSupport) {
            builder.setNeutralButton(resNeutral) { _, _ ->
                context?.let {
                    if (IdentityErrorDialogHelper.canOpenSupportEmail(it)) {
                        IdentityErrorDialogHelper.openSupportEmail(it,
                            resources,
                            supportEmail,
                            uriSession)
                    } else {
                        IdentityErrorDialogHelper.copyToClipboard(it,
                            title.toString(),
                            resources.getString(R.string.dialog_support_text,
                                uriSession,
                                BuildConfig.VERSION_NAME,
                                Build.VERSION.RELEASE))
                    }
                }
            }
            builder.setNegativeButton(resNegative) { _, _ ->
                dismiss()
            }
            builder.setPositiveButton(resPositive) { _, _ ->
                val intent = Intent(activity, IdentityCreateActivity::class.java)
                startActivity(intent)
            }
        }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        dialogFragmentListener?.onDialogResult(requestCode!!, Dialogs.DISMISSED, Intent())
    }

    //endregion
}
