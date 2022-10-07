package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.content.res.Resources
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import com.concordium.wallet.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*

class FindTokensBottomSheet(private var activity: Activity) {
    private lateinit var dialog: BottomSheetDialog
    private lateinit var title: TextView
    private lateinit var contractAddress: EditText
    private lateinit var pending: AppCompatImageView
    private lateinit var button: Button
    private lateinit var error: TextView
    private lateinit var search: SearchView
    private lateinit var tokenFound: ListView

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

        dialog.findViewById<TextView>(R.id.title)?.let {
            title = it
        }
        dialog.findViewById<EditText>(R.id.contract_address)?.let {
            contractAddress = it
        }
        dialog.findViewById<AppCompatImageView>(R.id.pending)?.let {
            pending = it
        }
        dialog.findViewById<Button>(R.id.look)?.let {
            button = it
        }
        dialog.findViewById<TextView>(R.id.error)?.let {
            error = it
        }
        dialog.findViewById<SearchView>(R.id.search)?.let {
            search = it
        }
        dialog.findViewById<ListView>(R.id.tokens_found)?.let {
            tokenFound = it
        }

        title.text = activity.getString(R.string.cis_find_tokens_title)

        button.setOnClickListener {
            button.isEnabled = false
            contractAddress.isEnabled = false
            pending.visibility = View.VISIBLE
            error.visibility = View.GONE
            look()
        }
    }

    fun show() {
        dialog.show()
    }

    private fun look() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(3000)
            activity.runOnUiThread {
                handleLookup()
            }
        }
    }

    private fun handleLookup() {
        val ok = true
        button.isEnabled = true
        pending.visibility = View.GONE
        if (ok) {
            title.text = activity.getString(R.string.cis_select_tokens_title)
            contractAddress.visibility = View.GONE
            search.visibility = View.VISIBLE
            tokenFound.visibility = View.VISIBLE
            button.text = activity.getString(R.string.cis_add_tokens)
            button.setOnClickListener {
                addTokens()
            }
        } else {
            contractAddress.setTextColor(activity.getColor(R.color.text_pink))
            contractAddress.setBackgroundResource(R.drawable.rounded_pink)
            //contractAddress.onEdit
            error.visibility = View.VISIBLE
        }
    }
// https://www.digitalocean.com/community/tutorials/android-searchview-example-tutorial
    private fun addTokens() {

    }
}
