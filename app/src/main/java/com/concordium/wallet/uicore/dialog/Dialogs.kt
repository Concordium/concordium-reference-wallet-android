package com.concordium.wallet.uicore.dialog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.concordium.wallet.App

class Dialogs {
    interface DialogFragmentListener {
        fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent)
    }

    companion object {
        const val POSITIVE = 0
        const val NEGATIVE = 1
        const val DISMISSED = 3
    }

    fun showOkDialog(
        activity: AppCompatActivity,
        requestCode: Int,
        titleId: Int,
        messageId: Int,
        positive: Int? = null
    ) {
        val resources = App.appContext.resources
        showOkDialog(
            activity,
            requestCode,
            resources.getString(titleId),
            resources.getString(messageId),
            if (positive != null) resources.getString(positive) else null
        )
    }

    fun showOkDialog(
        activity: AppCompatActivity,
        requestCode: Int,
        title: String,
        message: String,
        positive: String? = null
    ) {
        val fragment =
            CustomDialogFragment.createOkDialog(activity, requestCode, title, message, positive)
        fragment.show(activity.supportFragmentManager, CustomDialogFragment.TAG)
    }
}
