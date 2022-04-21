package com.concordium.wallet.ui.bakerdelegation.baker

import android.app.AlertDialog
import android.os.Bundle
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.util.Log
import kotlinx.android.synthetic.main.activity_baker_generate_keys_and_export.*

class BakerGenerateKeysAndExportActivity :
    BaseBakerActivity(R.layout.activity_baker_generate_keys_and_export, R.string.baker_registration_title) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        generateKeys()
    }

    override fun initViews() {
        baker_registration_export.setOnClickListener {
            startExport()
        }
    }

    private fun generateKeys() {
        viewModel.generateKeys()
        showNotice()
    }

    private fun showNotice() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.baker_registration_export_notice_title)
        builder.setMessage(getString(R.string.baker_registration_export_notice_message))
        builder.setPositiveButton(getString(R.string.baker_registration_export_notice_ok)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun startExport() {

    }
}