package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.concordium.wallet.CBORUtil
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivitySendTokenBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.SendTokenViewModel.Companion.SEND_TOKEN_DATA
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.ui.transaction.sendfunds.AddMemoActivity
import com.concordium.wallet.util.KeyboardUtil
import com.concordium.wallet.util.UnitConvertUtil
import com.concordium.wallet.util.getSerializable
import java.math.BigDecimal

class SendTokenActivity : BaseActivity() {
    private lateinit var binding: ActivitySendTokenBinding
    private val viewModel: SendTokenViewModel by viewModels()
    private val viewModelTokens: TokensViewModel by viewModels()
    private var searchTokenBottomSheet: SearchTokenBottomSheet? = null
    private val iconSize: Int get() = UnitConvertUtil.convertDpToPixel(resources.getDimension(R.dimen.list_item_height))

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val TOKEN = "TOKEN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendTokenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.sendTokenData.account = intent.getSerializable(ACCOUNT, Account::class.java)
        viewModel.sendTokenData.token = intent.getSerializable(TOKEN, Token::class.java)
        initObservers()
        updateWithToken(viewModel.sendTokenData.token)
        initViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.dispose()
    }

    private fun initViews() {
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.cis_send_funds)
        binding.amount.setText(CurrencyUtil.formatGTU(0, false))
        binding.atDisposal.text = CurrencyUtil.formatGTU(viewModel.sendTokenData.account?.getAtDisposalWithoutStakedOrScheduled(viewModel.sendTokenData.account?.totalUnshieldedBalance ?: 0) ?: 0, true)
        initializeAmount()
        initializeMax()
        initializeMemo()
        initializeReceiver()
        initializeAddressBook()
        initializeScanQrCode()
        initializeSend()
        initializeSearchToken()
        viewModel.getGlobalInfo()
        viewModel.getAccountBalance()
    }

    private fun initializeSend() {
        binding.send.setOnClickListener {
            send()
        }
    }

    private fun send() {
        val receiver = binding.receiver.text.toString()
        if (receiver.isEmpty()) {
            binding.receiver.setTextColor(ContextCompat.getColor(this, R.color.text_pink))
            binding.contractAddressError.text = getString(R.string.cis_enter_receiver_address)
            binding.contractAddressError.visibility = View.VISIBLE
        } else {
            binding.send.isEnabled = false
            viewModel.sendTokenData.amount = CurrencyUtil.toGTUValue(binding.amount.text.toString(), viewModel.sendTokenData.token) ?: BigDecimal.ZERO
            viewModel.sendTokenData.receiver = receiver
            binding.receiverName.let {
                if(it.visibility == View.VISIBLE){
                    viewModel.sendTokenData.receiverName = it.text?.toString()
                }else{
                    viewModel.sendTokenData.receiverName = null
                }
            }
            gotoReceipt()
        }
    }

    private fun initializeSearchToken() {
        binding.searchToken.searchToken.setOnClickListener {
            searchTokenBottomSheet = SearchTokenBottomSheet.newInstance(viewModel, viewModelTokens)
            searchTokenBottomSheet?.show(supportFragmentManager, "")
        }
    }

    private fun initializeAmount() {
        binding.amount.addTextChangedListener {
            viewModel.sendTokenData.amount = CurrencyUtil.toGTUValue(it.toString(), viewModel.sendTokenData.token) ?: BigDecimal.ZERO
            viewModel.loadTransactionFee()
            enableSend()
        }
        binding.amount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && (binding.amount.text.toString().replace(".", "").replace(",", "").toInt() == 0))
                binding.amount.setText("")
        }
        binding.amount.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    KeyboardUtil.hideKeyboard(this)
                    if (enableSend())
                        send()
                    true
                }
                else -> false
            }
        }
    }

    private fun initializeMax() {
        binding.max.isEnabled = false
        binding.max.setOnClickListener {
            var decimals = 6
            viewModel.sendTokenData.token?.let { token ->
                if (!token.isCCDToken)
                    decimals = token.tokenMetadata?.decimals?: 0
            }
            binding.amount.setText(CurrencyUtil.formatGTU(viewModel.sendTokenData.max ?: BigDecimal.ZERO, false, decimals))
            viewModel.sendTokenData.amount = CurrencyUtil.toGTUValue(it.toString(), viewModel.sendTokenData.token) ?: BigDecimal.ZERO
            enableSend()
        }
    }

    private fun enableSend(): Boolean {
        val amountText = binding.amount.text.toString().replace(",", "").replace(".", "").trim()
        if (amountText.isEmpty())
            binding.send.isEnabled = false
        else
            binding.send.isEnabled = (amountText.toLong() > 0) && viewModel.hasEnoughFunds()
        return binding.send.isEnabled
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
                    binding.memoClear.visibility = View.VISIBLE
                    handleMemo(memo)
                }
            }
        }

    private val getResultRecipient =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getSerializable(RecipientListActivity.EXTRA_RECIPIENT, Recipient::class.java)?.let { recipient ->
                    binding.receiver.text = recipient.address
                    binding.receiverName.let {view ->
                        view.visibility = View.VISIBLE
                        view.text = recipient.name
                    }
                    receiverAddressSet()
                }
            }
        }

    private val getResultScanQr =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(ScanQRActivity.EXTRA_BARCODE)?.let { barcode ->
                    binding.receiver.text = barcode
                    binding.receiverName.visibility = View.GONE
                    receiverAddressSet()
                }
            }
        }

    private fun handleMemo(memo: String) {
        if (memo.isNotEmpty()) {
            viewModel.setMemo(CBORUtil.encodeCBOR(memo))
            setMemoText(memo)
        } else {
            viewModel.setMemo(null)
            setMemoText("")
        }
    }

    private fun setMemoText(memo: String) {
        if (memo.isNotEmpty()) {
            binding.memo.text = memo
            binding.memoClear.visibility = View.VISIBLE
        } else {
            binding.memo.text = getString(R.string.send_funds_optional_add_memo)
            binding.memoClear.visibility = View.INVISIBLE
        }
    }

    private fun showPopupPaste(clipText: String) {
        val popupMenu = PopupMenu(this, binding.receiver)
        popupMenu.menuInflater.inflate(R.menu.paste_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.paste -> {
                    binding.receiver.text = clipText
                    binding.receiverName.visibility = View.GONE
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
            viewModel.sendTokenData.token = token
            binding.balanceTitle.text = getString(R.string.cis_token_balance, token.symbol).trim()
            val decimals: Int = if (token.isCCDToken) 6 else token.tokenMetadata?.decimals ?: 0
            binding.balance.text = CurrencyUtil.formatGTU(token.totalBalance, token.isCCDToken, decimals)
            binding.searchToken.tokenShortName.text = token.symbol
            if (token.isCCDToken) {
                Glide.with(this).load(R.drawable.ic_concordium_logo_no_text).into(binding.searchToken.tokenIcon)
            } else {
                token.tokenMetadata?.thumbnail?.let { thumbnail ->
                    Glide.with(this)
                        .load(thumbnail.url)
                        .placeholder(R.drawable.ic_token_loading_image)
                        .override(iconSize)
                        .fitCenter()
                        .error(R.drawable.ic_token_no_image)
                        .into(binding.searchToken.tokenIcon)
                }
            }
            binding.amount.setText(CurrencyUtil.formatGTU(0, false))
            viewModel.loadTransactionFee()
        }

        viewModel.feeReady.observe(this) { fee ->
            binding.fee.text = getString(R.string.cis_estimated_fee, CurrencyUtil.formatGTU(fee, true))
            binding.max.isEnabled = true
            if (!viewModel.hasEnoughFunds()) {
                binding.feeError.visibility = View.VISIBLE
                if (viewModel.sendTokenData.token != null && viewModel.sendTokenData.token!!.isCCDToken) {
                    binding.balanceTitle.setTextColor(getColor(R.color.text_black))
                    binding.balance.setTextColor(getColor(R.color.text_black))
                    binding.atDisposalTitle.setTextColor(getColor(R.color.text_pink))
                    binding.atDisposal.setTextColor(getColor(R.color.text_pink))
                } else {
                    binding.balanceTitle.setTextColor(getColor(R.color.text_pink))
                    binding.balance.setTextColor(getColor(R.color.text_pink))
                    binding.atDisposalTitle.setTextColor(getColor(R.color.text_black))
                    binding.atDisposal.setTextColor(getColor(R.color.text_black))
                }
            }
            else {
                binding.feeError.visibility = View.GONE
                binding.balanceTitle.setTextColor(getColor(R.color.text_black))
                binding.balance.setTextColor(getColor(R.color.text_black))
                binding.atDisposalTitle.setTextColor(getColor(R.color.text_black))
                binding.atDisposal.setTextColor(getColor(R.color.text_black))
            }
        }
        viewModel.errorInt.observe(this) {
            Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateWithToken(token: Token?) {
        token?.let {
            binding.balanceTitle.text = getString(R.string.cis_token_balance, it.symbol).trim()
            val decimals: Int = if (token.isCCDToken) 6 else token.tokenMetadata?.decimals ?: 0
            binding.balance.text = CurrencyUtil.formatGTU(it.totalBalance, it.isCCDToken, decimals)
            binding.searchToken.tokenShortName.text = it.symbol
            it.tokenMetadata?.thumbnail?.url?.let { url ->
                Glide.with(this).load(url).into(binding.searchToken.tokenIcon)
            }
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
