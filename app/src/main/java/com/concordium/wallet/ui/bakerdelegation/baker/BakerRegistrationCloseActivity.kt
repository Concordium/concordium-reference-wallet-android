package com.concordium.wallet.ui.bakerdelegation.baker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_KEYS
import com.concordium.wallet.data.util.FileUtil
import com.concordium.wallet.databinding.ActivityBakerRegistrationCloseBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.ui.more.export.ExportChooseMethodFragment
import com.concordium.wallet.uicore.dialog.AuthenticationDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BakerRegistrationCloseActivity : BaseDelegationBakerActivity() {
    private lateinit var binding: ActivityBakerRegistrationCloseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerRegistrationCloseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.baker_registration_title)
        initViews()
        generateKeys()
    }

    override fun initViews() {
        if (viewModel.bakerDelegationData.type == UPDATE_BAKER_KEYS)
            setActionBarTitle(R.string.baker_update_keys_settings_title)

        binding.bakerRegistrationExport.setOnClickListener {
            startExport()
        }

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                Toast.makeText(baseContext, getString(value), Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.bakerKeysLiveData.observe(this, Observer { bakerKeys ->
            binding.bakerRegistrationExportElectionVerifyKey.text = bakerKeys?.electionVerifyKey ?: ""
            binding.bakerRegistrationExportSignatureVerifyKey.text = bakerKeys?.signatureVerifyKey ?: ""
            binding.bakerRegistrationExportAggregationVerifyKey.text = bakerKeys?.aggregationVerifyKey ?: ""
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
        builder.setCancelable(false)
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
        if (requestCode == RESULT_SHARE_FILE) {
            continueToBakerConfirmation()
        } else if (resultCode == RESULT_OK && requestCode == RESULT_FOLDER_PICKER) {
            data?.data?.let { uri ->
                viewModel.saveFileToLocalFolder(uri)
                continueToBakerConfirmation()
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

    private fun continueToBakerConfirmation() {
        val intent = Intent(this, BakerRegistrationConfirmationActivity::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun errorLiveData(value: Int) {
    }
}
