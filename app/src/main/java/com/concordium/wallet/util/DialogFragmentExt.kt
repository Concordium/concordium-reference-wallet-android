package com.concordium.wallet.util

import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/**
 * Shows a single instance of the dialog,
 * dismissing the currently shown one if there is any.
 */
fun AppCompatDialogFragment.showSingle(
    fragmentManager: FragmentManager,
    tag: String,
) {
    fragmentManager.fragments.forEach { fragment ->
        if (fragment.tag == tag && fragment is DialogFragment && fragment::class == this::class) {
            fragment.dismissAllowingStateLoss()
        }
    }

    show(fragmentManager, tag)
}
