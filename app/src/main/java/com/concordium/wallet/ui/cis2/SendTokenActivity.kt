package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivitySendTokenBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.SendTokenViewModel.Companion.SEND_TOKEN_DATA
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.ui.transaction.sendfunds.AddMemoActivity
import com.concordium.wallet.util.getSerializable

class SendTokenActivity : BaseActivity() {
    private lateinit var binding: ActivitySendTokenBinding
    private val viewModel: SendTokenViewModel by viewModels()
    private var searchTokenBottomSheet: SearchTokenBottomSheet? = null

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val TOKEN = "TOKEN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendTokenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.sendTokenData.account = intent.getSerializable(ACCOUNT, Account::class.java)
        if (intent.hasExtra(TOKEN))
            viewModel.sendTokenData.token = intent.getSerializable(TOKEN, Token::class.java)
        initViews()
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        binding.send.isEnabled = true
    }

    private fun initViews() {
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.cis_send_funds)
        if (viewModel.sendTokenData.token != null) {
            binding.balanceTitle.text = getString(R.string.cis_token_balance, viewModel.sendTokenData.token?.shortName ?: "")
            binding.balance.text = CurrencyUtil.formatGTU(viewModel.sendTokenData.token?.balance ?: 0, true)
        } else {
            viewModel.sendTokenData.token = Token("default", "default", "DEF", 123)
        }
        binding.atDisposal.text = CurrencyUtil.formatGTU(viewModel.sendTokenData.account?.getAtDisposal() ?: 0,true)
        binding.amount.setText(CurrencyUtil.formatGTU(0, false))
        initializeSearchToken()
        initializeAmount()
        initializeMax()
        initializeMemo()
        initializeReceiver()
        initializeAddressBook()
        initializeScanQrCode()
        initializeSend()
    }

    private fun initializeSend() {
        binding.send.setOnClickListener {
            if (binding.receiver.text.toString().isNullOrEmpty()) {
                binding.receiver.setTextColor(ContextCompat.getColor(this, R.color.text_pink))
                binding.contractAddressError.text = getString(R.string.cis_enter_receiver_address)
                binding.contractAddressError.visibility = View.VISIBLE
            } else {
                binding.send.isEnabled = false
                viewModel.sendTokenData.amount = binding.amount.text.toString().replace(",", "").replace(".", "").toLong()
                viewModel.sendTokenData.receiver = binding.receiver.text.toString()
                viewModel.send()
            }
        }
    }

    private fun initializeSearchToken() {
        binding.searchToken.searchToken.setOnClickListener {
            searchTokenBottomSheet = SearchTokenBottomSheet()
            searchTokenBottomSheet?.show(supportFragmentManager, "")
        }
    }

    private fun initializeAmount() {
        binding.amount.addTextChangedListener {
            viewModel.loadTransactionFee()
        }
    }

    private fun initializeMax() {
        binding.max.setOnClickListener {
            binding.amount.setText(CurrencyUtil.formatGTU(viewModel.sendTokenData.token?.balance ?: 0, false))
        }
    }

    private fun initializeMemo() {
        viewModel.sendTokenData.token?.let {
            if (!it.isCCDToken()) {
                binding.memoContainer.visibility = View.GONE
            } else {
                binding.memo.setOnClickListener {
                    addMemo()
                }
                binding.memoClear.setOnClickListener {
                    binding.memo.text = getString(R.string.cis_optional_add_memo)
                    binding.memoClear.visibility = View.GONE
                }
            }
        }
    }

    private fun initializeReceiver() {
        binding.receiver.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                clipboard.primaryClipDescription?.let { clipDescription ->
                    if (clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || clipDescription.hasMimeType(
                            ClipDescription.MIMETYPE_TEXT_HTML)) {
                        clipboard.primaryClip?.getItemAt(0)?.text?.let { text ->
                            showPopupPaste(text.toString())
                        }
                    }
                }
            }
        }
    }

    private fun initializeAddressBook() {
        binding.addressBook.setOnClickListener {
            val intent = Intent(this, RecipientListActivity::class.java)
            intent.putExtra(RecipientListActivity.EXTRA_SELECT_RECIPIENT_MODE, true)
            intent.putExtra(RecipientListActivity.EXTRA_SHIELDED, viewModel.sendTokenData.account)
            intent.putExtra(RecipientListActivity.EXTRA_ACCOUNT, viewModel.sendTokenData.account)
            getResultRecipient.launch(intent)
        }
    }

    private fun initializeScanQrCode() {
        binding.scanQr.setOnClickListener {
            val intent = Intent(this, ScanQRActivity::class.java)
            intent.putExtra(ScanQRActivity.QR_MODE, ScanQRActivity.QR_MODE_CONCORDIUM_ACCOUNT)
            getResultScanQr.launch(intent)
        }
    }

    private fun addMemo() {
        getResultMemo.launch(Intent(this, AddMemoActivity::class.java))
    }

    private val getResultMemo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(AddMemoActivity.EXTRA_MEMO)?.let { memo ->
                    binding.memo.text = memo
                    binding.memoClear.visibility = View.VISIBLE
                }
            }
        }

    private val getResultRecipient =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getSerializable(RecipientListActivity.EXTRA_RECIPIENT, Recipient::class.java)?.let { recipient ->
                    binding.receiver.text = recipient.address
                    binding.receiver.setTextColor(ContextCompat.getColor(this, R.color.text_blue))
                    binding.contractAddressError.visibility = View.GONE
                }
            }
        }

    private val getResultScanQr =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(ScanQRActivity.EXTRA_BARCODE)?.let { barcode ->
                    binding.receiver.text = barcode
                    binding.receiver.setTextColor(ContextCompat.getColor(this, R.color.text_blue))
                    binding.contractAddressError.visibility = View.GONE
                }
            }
        }

    private fun showPopupPaste(clipText: String) {
        val popupMenu = PopupMenu(this, binding.receiver)
        popupMenu.menuInflater.inflate(R.menu.paste_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.paste -> {
                    binding.receiver.text = clipText
                    binding.receiver.setTextColor(ContextCompat.getColor(this, R.color.text_blue))
                    binding.contractAddressError.visibility = View.GONE
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
        viewModel.chooseToken.observe(this) { token ->
            searchTokenBottomSheet?.dismiss()
            searchTokenBottomSheet = null
            binding.searchToken.tokenShortName.text = token.shortName
        }
        viewModel.transactionReady.observe(this) {
            gotoReceipt()
        }
        viewModel.feeReady.observe(this) { fee ->
            binding.fee.text = getString(R.string.cis_estimated_fee, CurrencyUtil.formatGTU(fee, true))
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }

    private fun gotoReceipt() {
        val intent = Intent(this, SendTokenReceiptActivity::class.java)
        intent.putExtra(SEND_TOKEN_DATA, viewModel.sendTokenData)
        startActivity(intent)
    }
}
