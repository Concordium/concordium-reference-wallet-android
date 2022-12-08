package com.concordium.wallet.ui.more.export

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityExportTransactionLogBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.getSerializable

class ExportTransactionLogActivity : BaseActivity() {
    private lateinit var binding: ActivityExportTransactionLogBinding
    private val viewModel: ExportTransactionLogViewModel by viewModels()

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportTransactionLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.export_transaction_log_title)
        viewModel.account = intent.getSerializable(EXTRA_ACCOUNT, Account::class.java)
        initViews()
        initObservers()
        viewModel.onIdleRequested()
        val baseUrl =
            if (isStageNet) "https://api-ccdscan.stagenet.io/rest/export/"
            else if (isTestNet) "https://api-ccdscan.testnet.concordium.com/rest/export/"
            else "https://api-ccdscan.mainnet.concordium.software/rest/export/"
        //val baseUrl = "https://speed.hetzner.de/"
        viewModel.createRetrofitApi(baseUrl)
    }

    private fun initViews() {
        val ccdScanLink = if (isStageNet) "https://stagenet.ccdscan.io" else if (isTestNet) "https://testnet.ccdscan.io" else "https://ccdscan.io"
        binding.description.movementMethod = LinkMovementMethod.getInstance()
        binding.description.autoLinkMask = Linkify.WEB_URLS

        binding.description.text = getString(R.string.export_transaction_log_description, ccdScanLink)
        binding.generate.setOnClickListener {
            openFolderPicker(getResultFolderPicker)
        }
        binding.done.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initObservers() {
        viewModel.textResourceInt.observe(this) {
            Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
        }
        viewModel.downloadState.observe(this) { downloadState ->
            when (downloadState) {
                FileDownloadScreenState.Idle -> {
                    binding.downloadProgress.progress = 0
                    binding.bytesProgress.visibility = View.GONE
                }
                is FileDownloadScreenState.Downloading -> {
                    binding.downloadProgress.progress = downloadState.progress
                    binding.bytesProgress.text = "${downloadState.bytesProgress} / ${downloadState.bytesTotal} bytes"
                }
                is FileDownloadScreenState.Downloaded -> {
                    binding.downloadProgress.progress = 100
                    binding.bytesProgress.visibility = View.GONE
                    binding.description.text = getString(R.string.export_transaction_log_ready)
                    binding.done.isEnabled = true
                }
                is FileDownloadScreenState.Failed -> {
                    Toast.makeText(this, R.string.export_transaction_log_failed, Toast.LENGTH_LONG).show()
                    binding.done.isEnabled = true
                }
            }
        }
    }

    private val getResultFolderPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            it.data?.data?.let { destinationFolder ->
                binding.generate.visibility = View.GONE
                binding.done.visibility = View.VISIBLE
                binding.downloadProgress.visibility = View.VISIBLE
                binding.bytesProgress.visibility = View.VISIBLE
                binding.description.text = getString(R.string.export_transaction_log_generating)
                viewModel.downloadFile(destinationFolder)
            }
        }
    }
}
