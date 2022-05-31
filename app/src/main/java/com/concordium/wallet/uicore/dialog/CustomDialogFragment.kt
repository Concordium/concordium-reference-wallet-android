package com.concordium.wallet.uicore.dialog


import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.ui.common.identity.IdentityErrorDialogHelper
import com.concordium.wallet.ui.identity.identitycreate.IdentityCreateActivity
import com.concordium.wallet.ui.more.export.ExportActivity


class CustomDialogFragment : DialogFragment() {

    companion object {

        val TAG = "CustomDialogFragmentTag"
        val KEY_REQUEST_CODE = "key_request_code"
        val KEY_TYPE = "key_type"
        val KEY_TITLE = "key_title"
        val KEY_MESSAGE = "key_message"
        val KEY_POSITIVE = "key_positive"
        val KEY_NEUTRAL = "key_neutral"
        val KEY_NEGATIVE = "key_negative"
        val KEY_SUPPORT = "key_support"
        val KEY_SUPPORT_EMAIL = "key_support_email"

        val KEY_SUPPORT_TIMESTAMP = "key_support_timestamp"

        var dialogAccountFinalized: Dialog? = null;
        var dialogAccountFinalizedNoBackup: AlertDialog? = null;
        var dialogAccountFinalizedMap: HashMap<String, String> = HashMap<String, String>();

        //region Create/cancel dialogs
        //************************************************************

        fun dismissCustomDialog(activity: AppCompatActivity) {
            val dialogFragment = activity.supportFragmentManager.findFragmentByTag(TAG)
            if (dialogFragment is DialogFragment) {
                dialogFragment.dismiss()
                //dialogFragment?.dismissAllowingStateLoss();
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
            fragment.setArguments(args)
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


        fun newAccountFinalizedDialog(context:Context, accountName: String) {

            if (App.appCore.session.isAccountsBackedUp())
                return

            var title = context.getString(R.string.finalized_account_title_singular)
            var message = context.getString(R.string.finalized_account_message_singular, accountName)

            dialogAccountFinalizedMap.set(accountName, accountName)

            if(dialogAccountFinalized != null && dialogAccountFinalizedMap.count() > 1){ // we are already showing one dialog, meaning we finalised more accounts
                dialogAccountFinalized?.dismiss()
                title = context.getString(R.string.finalized_account_title_plural)
                message = context.getString(R.string.finalized_account_message_plural)
            }

            val builder = AlertDialog.Builder(context)
            builder.setCancelable(true)//This have to be set on dialog to have effect
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setNeutralButton(context.getString(R.string.finalized_account_ok),
                DialogInterface.OnClickListener { _, _ ->
                    dialogAccountFinalized?.dismiss()
                    dialogAccountFinalized = null
                    dialogAccountFinalizedMap.clear()

                    // Do you really not want to back up?!?!
                    showDoYouReallyNotWantToBackUp(context)

                })
            builder.setPositiveButton(context.getString(R.string.finalized_account_backup),
                DialogInterface.OnClickListener { _, _ ->
                    dialogAccountFinalized?.dismiss()
                    dialogAccountFinalized = null
                    dialogAccountFinalizedMap.clear()

                    val intent = Intent(context, ExportActivity::class.java)
                    context.startActivity(intent)
                })

            //Clear and dismiss any existing popups
            if(dialogAccountFinalized != null){
                dialogAccountFinalized?.dismiss()
                dialogAccountFinalized = null
            }

            dialogAccountFinalized = builder.create()
            dialogAccountFinalized?.setCanceledOnTouchOutside(false)
            dialogAccountFinalized?.show()
        }


        fun showAppUpdateBackupWarningDialog(context:Context) {

            var title = context.getString(R.string.app_update_account_no_backup_title_warning)
            var message = context.getString(R.string.app_update_account_no_backup_message)

            val builder = AlertDialog.Builder(context)
            builder.setCancelable(true)//This have to be set on dialog to have effect
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setNeutralButton(context.getString(R.string.app_update_account_no_backup_dismiss),
                DialogInterface.OnClickListener { _, _ ->
                    dialogAccountFinalized?.dismiss()
                    dialogAccountFinalized = null

                    showDoYouReallyNotWantToBackUp(context)
                })
            builder.setPositiveButton(context.getString(R.string.app_update_account_no_backup_now),
                DialogInterface.OnClickListener { _, _ ->
                    dialogAccountFinalized?.dismiss()
                    dialogAccountFinalized = null

                    val intent = Intent(context, ExportActivity::class.java)
                    context.startActivity(intent)
                })

            //Clear and dismiss any existing popups
            if(dialogAccountFinalized != null){
                dialogAccountFinalized?.dismiss()
                dialogAccountFinalized = null
            }

            dialogAccountFinalized = builder.create()
            dialogAccountFinalized?.setCanceledOnTouchOutside(false)
            dialogAccountFinalized?.show()
        }

        fun showDoYouReallyNotWantToBackUp(context:Context) {
            // Do you really not want to back up?!?!
            val builder = AlertDialog.Builder(context)
            builder.setCancelable(true)//This have to be set on dialog to have effect
            builder.setIcon(android.R.drawable.stat_sys_warning);
            builder.setTitle(R.string.finalized_account_no_backup_title_warning)
            builder.setMessage(R.string.finalized_account_no_backup_message)
            builder.setNeutralButton(context.getString(R.string.finalized_account_no_backup_dismiss),
                DialogInterface.OnClickListener { _, _ ->
                    dialogAccountFinalizedNoBackup?.dismiss()
                    dialogAccountFinalizedNoBackup = null
                })
            builder.setPositiveButton(context.getString(R.string.finalized_account_backup),
                DialogInterface.OnClickListener { _, _ ->
                    val intent = Intent(context, ExportActivity::class.java)
                    context.startActivity(intent)
                })
            dialogAccountFinalizedNoBackup = builder.create()
            dialogAccountFinalizedNoBackup?.setCanceledOnTouchOutside(false)
            dialogAccountFinalizedNoBackup?.show()
            //dialogAccountFinalizedNoBackup?.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(context.resources.getColor(R.color.text_green));
            dialogAccountFinalizedNoBackup?.getButton(AlertDialog.BUTTON_NEUTRAL)
                ?.setTextColor(Color.RED);
            dialogAccountFinalizedNoBackup?.let {
                val imageView: ImageView? = it.findViewById(android.R.id.icon)
                if (imageView != null) imageView.setColorFilter(
                    context.resources.getColor(R.color.warning_orange),
                    PorterDuff.Mode.SRC_IN
                )
            }
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
        val resources = App.appContext.getResources()
        val args: Bundle = requireArguments()
        requestCode = args.getInt(KEY_REQUEST_CODE, 0)
        val type = EDialogType.values()[args.getInt(KEY_TYPE, 0)]
        val title = args.getString(KEY_TITLE, "")
        val message = args.getString(KEY_MESSAGE, "")
        var resPositive = args.getString(KEY_POSITIVE, resources.getString(R.string.dialog_ok))
        var resNeutral = args.getString(KEY_NEUTRAL, null)
        var resNegative = args.getString(KEY_NEGATIVE, resources.getString(R.string.dialog_cancel))

        var uriSession = args.getString(KEY_SUPPORT, null)
        var supportEmail = args.getString(KEY_SUPPORT_EMAIL, null)
        var submissionTime = args.getString(KEY_SUPPORT_TIMESTAMP, null)
        if (type == EDialogType.YesNo) {
            resPositive = resources.getString(R.string.dialog_yes)
            resNegative = resources.getString(R.string.dialog_no)
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(true)//This have to be set on dialog to have effect
        builder.setTitle(title)
        //builder.setIcon(R.drawable.icon_info_dark);
        builder.setMessage(message)
        builder.setPositiveButton(resPositive,
            DialogInterface.OnClickListener { _, _ ->
                dialogFragmentListener?.onDialogResult(requestCode!!, Dialogs.POSITIVE, Intent())
            })
        if (type != EDialogType.OK && type != EDialogType.PositiveSupport) {
            builder.setNegativeButton(resNegative,
                DialogInterface.OnClickListener { _, _ ->
                    dialogFragmentListener?.onDialogResult(
                        requestCode!!,
                        Dialogs.NEGATIVE,
                        Intent()
                    )
                })
        }
        if (type == EDialogType.PositiveSupport) {
            builder.setNeutralButton(resNeutral,
                DialogInterface.OnClickListener { _, _ ->
                    context?.let{
                        if(IdentityErrorDialogHelper.canOpenSupportEmail(it)){
                            IdentityErrorDialogHelper.openSupportEmail(it, resources, supportEmail, uriSession)
                        }
                        else{
                            IdentityErrorDialogHelper.copyToClipboard(it, title.toString(), resources.getString(R.string.dialog_support_text, uriSession, BuildConfig.VERSION_NAME, Build.VERSION.RELEASE))
                        }
                    }
                })
            builder.setNegativeButton(resNegative,
                DialogInterface.OnClickListener { _, _ ->
                    dismiss()
                })
            builder.setPositiveButton(resPositive,
                DialogInterface.OnClickListener { _, _ ->
                    val intent = Intent(activity, IdentityCreateActivity::class.java)
                    startActivity(intent)
                })

        }

        val dialog = builder.create()
        //dialog.setOwnerActivity(getActivity());
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }



    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        dialogFragmentListener?.onDialogResult(requestCode!!, Dialogs.DISMISSED, Intent())
    }

    //endregion

}