package com.concordium.wallet.ui.bakerdelegation.baker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.util.FileUtil
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.more.export.ExportChooseMethodFragment
import com.concordium.wallet.uicore.dialog.AuthenticationDialogFragment
import kotlinx.android.synthetic.main.activity_baker_registration_close.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BakerRegistrationCloseActivity :
    BaseDelegationBakerActivity(R.layout.activity_baker_registration_close, R.string.baker_registration_title) {

    companion object {
        private const val RESULT_FOLDER_PICKER = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        generateKeys()
    }

    override fun initViews() {
        baker_registration_export.setOnClickListener {
            startExport()
        }

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                Toast.makeText(baseContext, getString(value), Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.bakerKeysLiveData.observe(this, Observer { bakerKeys ->
            baker_registration_export_election_verify_key.text = bakerKeys.electionVerifyKey
            baker_registration_export_signature_verify_key.text = bakerKeys.signatureVerifyKey
            baker_registration_export_aggregation_verify_key.text = bakerKeys.aggregationVerifyKey
        })

        viewModel.fileSavedLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                Toast.makeText(baseContext, getString(value), Toast.LENGTH_SHORT).show()
            }
        })
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
        chooseLocalFolderOrShareWithApp()
    }

    private fun chooseLocalFolderOrShareWithApp() {
        val dialogFragment = ExportChooseMethodFragment()
        dialogFragment.isCancelable = false
        dialogFragment.setCallback(object : ExportChooseMethodFragment.Callback {
            override fun onAnotherApp() {
                shareBakerFile()
            }
            override fun onLocalStorage() {
                openFolderPicker()
            }
        })
        dialogFragment.show(supportFragmentManager, AuthenticationDialogFragment.AUTH_DIALOG_TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == RESULT_FOLDER_PICKER) {
            data?.data?.let { uri ->
                viewModel.saveFileToLocalFolder(uri)
            }
        }
    }

    fun shareBakerFile() {
        CoroutineScope(Dispatchers.IO).launch {
            val bakerKeysJson = viewModel.bakerKeysJson()
            if (!bakerKeysJson.isNullOrEmpty()) {
                FileUtil.saveFile(App.appContext, DelegationBakerViewModel.FILE_NAME_BAKER_KEYS, bakerKeysJson)
                shareFile(viewModel.getTempFileWithPath())
            }
        }
    }
}
