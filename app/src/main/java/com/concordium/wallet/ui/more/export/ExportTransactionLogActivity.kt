package com.concordium.wallet.ui.more.export

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityExportTransactionLogBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.getSerializable
import java.text.NumberFormat

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
                }
                is FileDownloadScreenState.Downloading -> {
                    binding.description.text = getString(R.string.export_transaction_log_downloading)
                    binding.downloadProgress.visibility = View.VISIBLE
                    binding.downloadProgress.progress = downloadState.progress
                    binding.bytesProgress.visibility = View.VISIBLE
                    val kbProgress = NumberFormat.getIntegerInstance().format((downloadState.bytesProgress / 1024)).toString()
                    val kbTotal = NumberFormat.getIntegerInstance().format((downloadState.bytesTotal / 1024)).toString()
                    binding.bytesProgress.text = getString(R.string.export_transaction_log_progress, kbProgress, kbTotal)
                    binding.statusImageview.visibility = View.GONE
                }
                is FileDownloadScreenState.Downloaded -> {
                    binding.description.text = getString(R.string.export_transaction_log_saved)
                    binding.downloadProgress.progress = 100
                    binding.bytesProgress.visibility = View.GONE
                    binding.done.isEnabled = true
                }
                is FileDownloadScreenState.Failed -> {
                    binding.description.text = getString(R.string.export_transaction_log_failed)
                    binding.description.setTextColor(getColor(R.color.text_pink))
                    binding.downloadProgress.progressDrawable.setColorFilter(ContextCompat.getColor(this, R.color.text_pink), PorterDuff.Mode.MULTIPLY)
                    binding.bytesProgress.visibility = View.GONE
                    binding.statusImageview.visibility = View.GONE
                    binding.done.isEnabled = true
                }
            }
        }
    }

    private val getResultFolderPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            it.data?.data?.let { destinationFolder ->
                binding.description.text = getString(R.string.export_transaction_log_generating)
                binding.generate.visibility = View.GONE
                binding.done.visibility = View.VISIBLE
                binding.statusImageview.visibility = View.VISIBLE
                viewModel.downloadFile(destinationFolder)
            }
        }
    }
}
