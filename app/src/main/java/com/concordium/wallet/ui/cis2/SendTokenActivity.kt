package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Thumbnail
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivitySendTokenBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.SendTokenViewModel.Companion.SEND_TOKEN_DATA
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.ui.transaction.sendfunds.AddMemoActivity
import com.concordium.wallet.util.getSerializable
import javax.crypto.Cipher

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
        initObservers()
        /*
        if (intent.hasExtra(TOKEN)) {
            viewModel.sendTokenData.token = intent.getSerializable(TOKEN, Token::class.java)
            updateWithToken(viewModel.sendTokenData.token)
            initViews()
            viewModel.loadTransactionFee()
        } else {
            viewModel.loadCCDDefaultToken(viewModel.sendTokenData.account?.address ?: "")
        }
        */
        viewModel.sendTokenData.token = Token("1084", "00", "100",
            TokenMetadata(6, "Optimal FT metadata values. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
            "CIS2-Multi.transfer", "FT",
            Thumbnail("https://developer.concordium.software/en/mainnet/_images/wCCD.svg"),false), false, "1696", false, 1000000000000, 200000005, "CIS2-Multi")

        updateWithToken(viewModel.sendTokenData.token)
        initViews()
        viewModel.loadTransactionFee()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.dispose()
    }

    private fun initViews() {
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.cis_send_funds)
        binding.amount.setText(CurrencyUtil.formatGTU(0, false))
        initializeAmount()
        initializeMax()
        initializeMemo()
        initializeReceiver()
        initializeAddressBook()
        initializeScanQrCode()
        initializeSend()
        initializeSearchToken()
        viewModel.getGlobalInfo()
    }

    private fun initializeSend() {
        binding.send.setOnClickListener {
            if (binding.receiver.text.toString().isEmpty()) {
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
            searchTokenBottomSheet = SearchTokenBottomSheet.newInstance(viewModel)
            searchTokenBottomSheet?.show(supportFragmentManager, "")
        }
    }

    private fun initializeAmount() {
        binding.amount.addTextChangedListener {
            viewModel.loadTransactionFee()
            enableSend()
        }
    }

    private fun initializeMax() {
        binding.max.isEnabled = false
        binding.max.setOnClickListener {
            binding.amount.setText(CurrencyUtil.formatGTU(viewModel.sendTokenData.max ?: 0, false))
            enableSend()
        }
    }

    private fun enableSend() {
        val amountText = binding.amount.text.toString().replace(",", "").replace(".", "").trim()
        if (amountText.isEmpty())
            binding.send.isEnabled = false
        else
            binding.send.isEnabled = (amountText.toLong() > 0)
    }

    private fun initializeMemo() {
        viewModel.sendTokenData.token?.let {
            if (!it.isCCDToken) {
                binding.memoContainer.visibility = View.GONE
            } else {
                binding.memo.setOnClickListener {
                    addMemo()
                }
                binding.memoClear.setOnClickListener {
                    binding.memo.text = getString(R.string.cis_optional_add_memo)
                    binding.memoClear.visibility = View.GONE
                    viewModel.sendTokenData.memo = null
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
                    viewModel.sendTokenData.memo = memo
                }
            }
        }

    private val getResultRecipient =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getSerializable(RecipientListActivity.EXTRA_RECIPIENT, Recipient::class.java)?.let { recipient ->
                    binding.receiver.text = recipient.address
                    receiverAddressSet()
                }
            }
        }

    private val getResultScanQr =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(ScanQRActivity.EXTRA_BARCODE)?.let { barcode ->
                    binding.receiver.text = barcode
                    receiverAddressSet()
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
                    receiverAddressSet()
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun receiverAddressSet() {
        binding.receiver.setTextColor(ContextCompat.getColor(this, R.color.text_blue))
        binding.contractAddressError.visibility = View.GONE
        viewModel.sendTokenData.receiver = binding.receiver.text.toString()
        viewModel.loadTransactionFee()
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
        viewModel.chooseToken.observe(this) { token ->
            searchTokenBottomSheet?.dismiss()
            searchTokenBottomSheet = null
            binding.searchToken.tokenShortName.text = token.token
        }
        viewModel.transactionReady.observe(this) {
            gotoReceipt()
        }
        viewModel.feeReady.observe(this) { fee ->
            binding.fee.text = getString(R.string.cis_estimated_fee, CurrencyUtil.formatGTU(fee, true))
            binding.max.isEnabled = true
        }
        viewModel.defaultToken.observe(this) { token ->
            viewModel.sendTokenData.token = token
            initViews()
            updateWithToken(token)
            viewModel.loadTransactionFee()
        }
        viewModel.errorInt.observe(this) {
            Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
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
                }
            })
        }
    }

    private fun updateWithToken(token: Token?) {
        token?.let {
            binding.balanceTitle.text = getString(R.string.cis_token_balance, it.token)
            binding.balance.text = CurrencyUtil.formatGTU(it.totalBalance, true)
            binding.atDisposal.text = CurrencyUtil.formatGTU(it.atDisposal,true)
            binding.searchToken.tokenShortName.text = it.token
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
