package com.concordium.wallet.ui.cis2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
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
import javax.crypto.Cipher

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
        binding.amount.text = CurrencyUtil.formatGTU(viewModel.sendTokenData.amount, viewModel.sendTokenData.token)
        binding.receiver.text = viewModel.sendTokenData.receiver
        viewModel.sendTokenData.receiverName?.let {
            binding.receiverName.visibility = View.VISIBLE
            binding.receiverName.text= it
        }
        CoroutineScope(Dispatchers.Default).launch {
            runOnUiThread {
                showPageAsSendPrompt()
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
        } else {
            val symbol: String = viewModel.sendTokenData.token!!.tokenMetadata?.symbol ?: ""
            if (symbol.isNotBlank())
                binding.token.text = symbol
            else {
                binding.tokenTitle.visibility = View.GONE
                binding.token.visibility = View.GONE
            }
        }
    }

    private fun onSend() {
        binding.sendFunds.isEnabled = false
        viewModel.send()
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

        viewModel.showAuthentication.observe(this) {
            showAuthentication(authenticateText(), object : AuthenticationCallback {
                override fun getCipherForBiometrics() : Cipher? {
                    return viewModel.getCipherForBiometrics()
                }
                override fun onCorrectPassword(password: String) {
                    viewModel.continueWithPassword(password)
                }
                override fun onCipher(cipher: Cipher) {
                    viewModel.checkLogin(cipher)
                }
                override fun onCancelled() {
                    binding.sendFunds.isEnabled = true
                }
            })
        }

        viewModel.transactionReady.observe(this) {submissionId ->
            binding.includeTransactionSubmittedNo.transactionSubmittedDivider.visibility = View.VISIBLE
            binding.includeTransactionSubmittedNo.transactionSubmittedId.let {view ->
                view.visibility = View.VISIBLE
                view.text = submissionId
            }
            showPageAsReceipt()
        }

        viewModel.errorInt.observe(this) {
            Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
            binding.sendFunds.isEnabled = true
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

    private fun showPageAsSendPrompt() {
        receiptMode = false
        binding.sendFunds.visibility = View.VISIBLE
        binding.finish.visibility = View.GONE
        binding.header.visibility = View.VISIBLE
        binding.includeTransactionSubmittedHeader.transactionSubmitted.visibility = View.GONE
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressLayout.visibility = if (waiting) View.VISIBLE else View.GONE
    }
}
