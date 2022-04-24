package com.concordium.wallet.ui.more.export

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.concordium.wallet.R

class ExportChooseMethodFragment : DialogFragment() {

    private var callback: Callback? = null

    interface Callback {
        fun onAnotherApp()
        fun onLocalStorage()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.export_method_question))
            .setNegativeButton(getString(R.string.export_method_another_app)) { _,_ -> callback?.onAnotherApp() }
            .setPositiveButton(getString(R.string.export_method_local_storage)) { _,_ -> callback?.onLocalStorage() }
            .create()

    fun setCallback(callback: Callback) {
        this.callback = callback
    }
}
