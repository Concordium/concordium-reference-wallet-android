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
import java.math.BigInteger

class SendTokenActivity : BaseActivity() {
    private lateinit var binding: ActivitySendTokenBinding
    private val viewModel: SendTokenViewModel by viewModels()
    private val viewModelTokens: TokensViewModel by viewModels()
    private var searchTokenBottomSheet: SearchTokenBottomSheet? = null
    private val iconSize: Int get() = UnitConvertUtil.convertDpToPixel(resources.getDimension(R.dimen.list_item_height))

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val TOKEN = "TOKEN"
        const val SEND_ONLY_SELECTED_TOKEN = "SEND_ONLY_SELECTED_TOKEN"
        const val TOKEN_TRANSFER_FLOW_FINISHED = "TOKEN_TRANSFER_FLOW_FINISHED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendTokenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.sendTokenData.account = intent.getSerializable(ACCOUNT, Account::class.java)
        viewModel.chooseToken.postValue(intent.getSerializable(TOKEN, Token::class.java))
        viewModel.sendOnlySelectedToken = intent.getBooleanExtra(SEND_ONLY_SELECTED_TOKEN, false)
        initObservers()
        initViews()
        enableSend()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra(TOKEN_TRANSFER_FLOW_FINISHED, false) == true)
            onBackPressedDispatcher.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        enableSend()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.dispose()
    }

    private fun initViews() {
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.cis_send_funds
        )
        binding.amount.setText(CurrencyUtil.formatGTU(BigInteger.ZERO, false))
        binding.atDisposal.text = CurrencyUtil.formatGTU(
            viewModel.sendTokenData.account?.getAtDisposalWithoutStakedOrScheduled(
                viewModel.sendTokenData.account?.totalUnshieldedBalance ?: BigInteger.ZERO
            ) ?: BigInteger.ZERO, true
        )
        initializeAmount()
        initializeMax()
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
            viewModel.sendTokenData.receiver = receiver
            gotoReceipt()
        }
    }

    private fun initializeSearchToken() {
        if (viewModel.sendOnlySelectedToken) {
            binding.searchToken.searchIcon.visibility = View.INVISIBLE
        } else {
            binding.searchToken.searchIcon.visibility = View.VISIBLE
            binding.searchToken.searchToken.setOnClickListener {
                searchTokenBottomSheet =
                    SearchTokenBottomSheet.newInstance(viewModel, viewModelTokens)
                searchTokenBottomSheet?.show(supportFragmentManager, "")
            }
        }
    }

    private fun initializeAmount() {
        binding.amount.addTextChangedListener {
            viewModel.sendTokenData.amount =
                CurrencyUtil.toGTUValue(it.toString(), viewModel.sendTokenData.token)
                    ?: BigInteger.ZERO
            viewModel.loadTransactionFee()
            enableSend()
        }
        binding.amount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && viewModel.sendTokenData.amount.signum() == 0)
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
        binding.max.setOnClickListener {
            var decimals = 6
            viewModel.sendTokenData.token?.let { token ->
                if (!token.isCCDToken)
                    decimals = token.tokenMetadata?.decimals ?: 0
            }
            // Ensure that max is not negative
            val max = BigInteger.ZERO.max(viewModel.sendTokenData.max ?: BigInteger.ZERO)
            binding.amount.setText(CurrencyUtil.formatGTU(max, false, decimals))
            enableSend()
        }
    }

    private fun enableSend(): Boolean {
        binding.send.isEnabled =
            viewModel.sendTokenData.amount.signum() > 0
                    && viewModel.sendTokenData.fee != null
                    && viewModel.hasEnoughFunds()
                    && viewModel.sendTokenData.receiver.isNotEmpty()
                    && viewModel.isReceiverAddressValid.value == true
        return binding.send.isEnabled
    }

    private fun initializeReceiver() {
        binding.receiver.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                clipboard.primaryClipDescription?.let { clipDescription ->
                    if (clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                        clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
                    ) {
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
                    handleMemo(memo)
                }
            }
        }

    private val getResultRecipient =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getSerializable(
                    RecipientListActivity.EXTRA_RECIPIENT,
                    Recipient::class.java
                )?.let { recipient ->
                    binding.receiver.text = recipient.address
                    binding.receiverName.visibility = View.VISIBLE
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

    private fun clearMemo() =
        handleMemo("")

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
            binding.memoClear.visibility = View.GONE
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
            binding.balanceTitle.text = getString(R.string.cis_token_balance, token.symbol).trim()
            val decimals: Int = if (token.isCCDToken) 6 else token.tokenMetadata?.decimals ?: 0
            binding.balance.text =
                CurrencyUtil.formatGTU(token.totalBalance, token.isCCDToken, decimals)
            binding.searchToken.tokenShortName.text =
                token.symbol.ifBlank { token.tokenMetadata?.name }
            if (token.isCCDToken) {
                Glide.with(this).load(R.drawable.ic_concordium_logo_no_text)
                    .into(binding.searchToken.tokenIcon)
            } else {
                token.tokenMetadata?.thumbnail.let { thumbnail ->
                    Glide.with(this)
                        .load(thumbnail?.url)
                        .placeholder(R.drawable.ic_token_loading_image)
                        .override(iconSize)
                        .fitCenter()
                        .error(R.drawable.ic_token_no_image)
                        .into(binding.searchToken.tokenIcon)
                }
            }
            binding.amount.setText(CurrencyUtil.formatGTU(BigInteger.ZERO, false))
            binding.amount.decimals = token.tokenMetadata?.decimals ?: 6
            // For non-CCD tokens Max is always available.
            binding.max.isEnabled = !token.isCCDToken

            if (!token.isCCDToken) {
                binding.memoContainer.visibility = View.GONE
            } else {
                binding.memoContainer.visibility = View.VISIBLE
                binding.memo.setOnClickListener {
                    addMemo()
                }
                binding.memoClear.setOnClickListener {
                    clearMemo()
                }
            }
            // This also initiates fee loading.
            clearMemo()
        }

        viewModel.feeReady.observe(this) { fee ->
            // Null value means the fee is outdated.
            binding.fee.text =
                if (fee != null)
                    getString(R.string.cis_estimated_fee, CurrencyUtil.formatGTU(fee, true))
                else
                    ""
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
            } else {
                binding.feeError.visibility = View.GONE
                binding.balanceTitle.setTextColor(getColor(R.color.text_black))
                binding.balance.setTextColor(getColor(R.color.text_black))
                binding.atDisposalTitle.setTextColor(getColor(R.color.text_black))
                binding.atDisposal.setTextColor(getColor(R.color.text_black))
            }
            enableSend()
        }
        viewModel.errorInt.observe(this) {
            Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
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
