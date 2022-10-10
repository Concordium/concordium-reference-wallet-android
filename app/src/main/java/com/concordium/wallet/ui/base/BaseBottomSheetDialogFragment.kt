package com.concordium.wallet.ui.base

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.concordium.wallet.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window?.attributes?.windowAnimations = R.style.Animations_BottomSheet
        dialog?.setOnShowListener { dialog ->
            val height = (Resources.getSystem().displayMetrics.heightPixels * 0.95).toInt()
            dialog as BottomSheetDialog
            dialog.behavior.peekHeight = height
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            val content = dialog.findViewById<ConstraintLayout>(R.id.content)
            val params = content?.layoutParams
            params?.height = height
            content?.layoutParams = params
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme
}