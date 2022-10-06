package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.content.res.Resources
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.concordium.wallet.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class FindTokensBottomSheet(private var activity: Activity) {
    private lateinit var dialog: BottomSheetDialog

    fun initialize() {
        val height = (Resources.getSystem().displayMetrics.heightPixels * 0.95).toInt()

        dialog = BottomSheetDialog(activity, R.style.BottomSheetDialogTheme)
        dialog.behavior.peekHeight = height
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.window?.attributes?.windowAnimations = R.style.Animations_BottomSheet
        dialog.setContentView(R.layout.dialog_find_tokens)

        val content = dialog.findViewById<ConstraintLayout>(R.id.content)
        val params = content?.layoutParams
        params?.height = height
        content?.layoutParams = params

        val title = dialog.findViewById<TextView>(R.id.title)
        title?.text = activity.getString(R.string.cis_find_tokens_title)

        val look = dialog.findViewById<Button>(R.id.look)
        look?.setOnClickListener {

        }
    }

    fun show() {
        dialog.show()
    }
}
