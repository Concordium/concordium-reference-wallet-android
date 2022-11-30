package com.concordium.wallet.ui.cis2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivitySendTokenReceiptBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.SendTokenViewModel.Companion.SEND_TOKEN_DATA
import com.concordium.wallet.util.getSerializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SendTokenReceiptActivity : BaseActivity() {
    private lateinit var binding: ActivitySendTokenReceiptBinding
    private val viewModel: SendTokenViewModel by viewModels()
    private var receiptMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.sendTokenData = intent.getSerializable(SEND_TOKEN_DATA, SendTokenData::class.java)
        binding = ActivitySendTokenReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.cis_send_funds)
        initViews()
        initObservers()
        viewModel.loadTransactionFee()
    }

    override fun onBackPressed() {
        if (!receiptMode)
            super.onBackPressed()
    }

    private fun initViews() {
        binding.sender.text = viewModel.sendTokenData.account?.name.plus("\n\n").plus(viewModel.sendTokenData.account?.address)
        binding.amount.text = CurrencyUtil.formatGTU(viewModel.sendTokenData.amount, false)
        binding.token.text = viewModel.sendTokenData.token?.token ?: ""
        binding.receiver.text = viewModel.sendTokenData.receiver
        CoroutineScope(Dispatchers.Default).launch {
            runOnUiThread {
                showPageAsReceipt()
            }
        }
        binding.sendFunds.setOnClickListener {
            onSend()
        }
        binding.finish.setOnClickListener {
            onFinish()
        }
        if (viewModel.sendTokenData.token!!.isCCDToken) {
            binding.tokenTitle.visibility = View.GONE
            binding.token.visibility = View.GONE
        }
    }

    private fun onSend() {
    }

    private fun onFinish() {
        finish()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
        viewModel.feeReady.observe(this) { fee ->
            binding.fee.text = getString(R.string.cis_estimated_fee, CurrencyUtil.formatGTU(fee, true))
        }
    }

    private fun showPageAsReceipt() {
        receiptMode = true
        hideActionBarBack()
        binding.sendFunds.visibility = View.GONE
        binding.finish.visibility = View.VISIBLE
        binding.header.visibility = View.GONE
        binding.includeTransactionSubmittedHeader.transactionSubmitted.visibility = View.VISIBLE
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressLayout.visibility = if (waiting) View.VISIBLE else View.GONE
    }
}
