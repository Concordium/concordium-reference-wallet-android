package com.concordium.wallet.ui.transaction.sendfunds

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.CBORUtil
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivitySendFundsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.failed.FailedActivity
import com.concordium.wallet.ui.common.failed.FailedViewModel
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.ui.transaction.sendfundsconfirmed.SendFundsConfirmedActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil
import com.concordium.wallet.util.getSerializable
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import javax.crypto.Cipher

class SendFundsActivity : BaseActivity() {
    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val EXTRA_RECIPIENT = "EXTRA_RECIPIENT"
    }

    private lateinit var binding: ActivitySendFundsBinding
    private lateinit var viewModel: SendFundsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendFundsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.send_funds_title)

        val account = intent.getSerializable(EXTRA_ACCOUNT, Account::class.java)
        val isShielded = intent.extras!!.getBoolean(EXTRA_SHIELDED)
        initializeViewModel()
        viewModel.initialize(account, isShielded)
        handleRecipientIntent(intent)
        initViews()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleRecipientIntent(intent)
    }

    private fun handleRecipientIntent(intent: Intent?) {
        if (intent?.hasExtra(EXTRA_RECIPIENT) == true) {
            val recipient = intent.getSerializable(EXTRA_RECIPIENT, Recipient::class.java)
            handleRecipient(recipient)
        }
    }

    fun handleRecipient(recipient: Recipient?){
        recipient?.let {
            viewModel.selectedRecipient = recipient
            updateConfirmButton()
            if(viewModel.account.id == recipient.id){
                setActionBarTitle(if(viewModel.isShielded) R.string.send_funds_unshield_title else R.string.send_funds_shield_title)
                if (!viewModel.isShielded)
                    binding.sendAll.visibility = View.GONE
            }
        }
    }

    private fun handleMemo(memo: String?) {
        if (memo != null && memo.isNotEmpty()) {
            viewModel.setMemo(CBORUtil.encodeCBOR(memo))
            setMemoText(memo)
        } else {
            viewModel.setMemo(null)
            setMemoText("")
        }
    }

    private fun setMemoText(txt: String) {
        if (txt.isNotEmpty()) {
            binding.memoTextview.text = txt
            binding.memoClear.visibility = View.VISIBLE
        }
        else {
            binding.memoTextview.text = getString(R.string.send_funds_optional_add_memo)
            binding.memoClear.visibility = View.INVISIBLE
        }
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[SendFundsViewModel::class.java]
        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(isWaiting())
            }
        }
        viewModel.waitingReceiverAccountPublicKeyLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(isWaiting())
            }
        }
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(createConfirmString(), object : AuthenticationCallback {
                        override fun getCipherForBiometrics() : Cipher?{
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
        })
        viewModel.gotoSendFundsConfirmLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    gotoSendFundsConfirm()
                }
            }
        })
        viewModel.gotoFailedLiveData.observe(
            this,
            object : EventObserver<Pair<Boolean, BackendError?>>() {
                override fun onUnhandledEvent(value: Pair<Boolean, BackendError?>) {
                    if (value.first) {
                        gotoFailed(value.second)
                    }
                }
            })
        viewModel.transactionFeeLiveData.observe(this) { value ->
            value?.let {
                binding.feeInfoTextview.visibility = View.VISIBLE
                binding.feeInfoTextview.text = getString(
                    R.string.send_funds_fee_info, CurrencyUtil.formatGTU(value)
                )
                updateConfirmButton()
            }
        }
        viewModel.recipientLiveData.observe(this
        ) { value -> showRecipient(value) }
    }

    private fun initViews() {
        binding.includeProgress.progressLayout.visibility = View.GONE
        binding.errorTextview.visibility = View.INVISIBLE
        binding.amountEdittext.afterTextChanged {
            if (binding.amountEdittext.hasFocus()) { //If it does not have focus, it means we are injecting text programatically
                viewModel.disableSendAllValue()
            }
            updateConfirmButton()
            updateAmountEditText()
        }
        updateAmountEditText()
        binding.amountEdittext.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    viewModel.selectedRecipient?.let {
                        if (viewModel.validateAndSaveRecipient(it.address)) {
                            if (enableConfirm())
                                viewModel.sendFunds(binding.amountEdittext.text.toString())
                            else
                                KeyboardUtil.hideKeyboard(this)
                        }
                    }
                    true
                }
                else -> false
            }
        }
        binding.sendFundsPasteRecipient.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                val address = editable.toString().trim()
                if (viewModel.selectedRecipient == null) {
                    handleRecipient(Recipient(0,"", address))
                }
                if (address != viewModel.selectedRecipient?.address) {
                    handleRecipient(Recipient(0,"", address))
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        if(viewModel.isTransferToSameAccount()){
            binding.memoContainer.visibility = View.GONE
        }
        else{
            binding.memoContainer.visibility = View.VISIBLE

            binding.memoContainer.setOnClickListener {
                if(viewModel.showMemoWarning()){
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(getString(R.string.transaction_memo_warning_title))
                    builder.setMessage(getString(R.string.transaction_memo_warning_text))
                    builder.setNegativeButton(getString(R.string.transaction_memo_warning_dont_show)) { _, _ ->
                        viewModel.dontShowMemoWarning()
                        gotoEnterMemo()
                    }
                    builder.setPositiveButton(getString(R.string.transaction_memo_warning_ok)) { _, _ ->
                        gotoEnterMemo()
                    }
                    builder.setCancelable(true)
                    builder.create().show()
                }
                else {
                    gotoEnterMemo()
                }
            }

            setMemoText("")

            binding.memoClear.setOnClickListener {
                viewModel.setMemo(null)
                setMemoText("")
            }
        }

        binding.sendAll.setOnClickListener {
            viewModel.updateSendAllValue()
        }

        viewModel.sendAllAmountLiveData.observe(this) { value ->
            value?.let {
                binding.amountEdittext.setText(CurrencyUtil.formatGTU(value))
            }
        }

        binding.searchRecipientLayout.setOnClickListener {
            gotoSelectRecipient()
        }
        binding.scanQrRecipientLayout.setOnClickListener {
            gotoScanBarCode()
        }

        binding.confirmButton.setOnClickListener {
            viewModel.selectedRecipient?.let {
                if (viewModel.validateAndSaveRecipient(it.address)) {
                    if (viewModel.account.address == it.address && !viewModel.isShielded)
                        check95PercentWarning()
                    else
                        sendFunds()
                }
            }
        }

        if (viewModel.isShielded) {
            binding.balanceTotalText.text = getString(R.string.accounts_overview_balance_at_disposal)
            binding.atDisposalTotalText.text = getString(R.string.accounts_overview_shielded_balance)
            binding.balanceTotalTextview.text = CurrencyUtil.formatGTU(viewModel.account.getAtDisposalWithoutStakedOrScheduled(viewModel.account.totalUnshieldedBalance), withGStroke = true)
            binding.atDisposalTotalTextview.text = CurrencyUtil.formatGTU(viewModel.account.totalShieldedBalance, withGStroke = true)
        }
        else {
            binding.balanceTotalText.text = getString(R.string.accounts_overview_account_total)
            binding.atDisposalTotalText.text = getString(R.string.accounts_overview_at_disposal)
            binding.balanceTotalTextview.text = CurrencyUtil.formatGTU(viewModel.account.totalUnshieldedBalance, withGStroke = true)
            binding.atDisposalTotalTextview.text = CurrencyUtil.formatGTU(viewModel.account.getAtDisposalWithoutStakedOrScheduled(viewModel.account.totalUnshieldedBalance), withGStroke = true)
        }
    }

    private fun check95PercentWarning() {
        val amountValue = CurrencyUtil.toGTUValue(binding.amountEdittext.text.toString())
        val atDisposal = viewModel.account.getAtDisposalWithoutStakedOrScheduled(viewModel.account.totalUnshieldedBalance)
        if (amountValue != null) {
            if (amountValue > atDisposal * BigDecimal(0.95)) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.send_funds_more_than_95_title)
                builder.setMessage(getString(R.string.send_funds_more_than_95_message))
                builder.setPositiveButton(getString(R.string.send_funds_more_than_95_continue)) { _, _ -> sendFunds() }
                builder.setNegativeButton(getString(R.string.send_funds_more_than_95_new_stake)) { dialog, _ -> dialog.dismiss() }
                builder.create().show()
            }
            else
                sendFunds()
        }
    }

    private fun sendFunds() {
        viewModel.sendFunds(binding.amountEdittext.text.toString())
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressLayout.visibility = if(waiting) View.VISIBLE else View.GONE
        // Update button enabled state, because it is dependant on waiting state
        updateConfirmButton()
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(binding.rootLayout, stringRes)
    }

    private fun gotoSelectRecipient() {
        val intent = Intent(this, RecipientListActivity::class.java)
        intent.putExtra(RecipientListActivity.EXTRA_SELECT_RECIPIENT_MODE, true)
        intent.putExtra(RecipientListActivity.EXTRA_SHIELDED, viewModel.isShielded)
        intent.putExtra(RecipientListActivity.EXTRA_ACCOUNT, viewModel.account)
        getResultRecipient.launch(intent)
    }

    private val getResultRecipient =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getSerializable(RecipientListActivity.EXTRA_RECIPIENT, Recipient::class.java)?.let { recipient ->
                    handleRecipient(recipient)
                }
            }
        }

    private fun gotoScanBarCode() {
        val intent = Intent(this, ScanQRActivity::class.java)
        intent.putExtra(ScanQRActivity.QR_MODE, ScanQRActivity.QR_MODE_CONCORDIUM_ACCOUNT)
        getResultScanQr.launch(intent)
    }

    private val getResultScanQr =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(ScanQRActivity.EXTRA_BARCODE)?.let { barcode ->
                    handleRecipient(Recipient(0,"", barcode))
                }
            }
        }

    private fun gotoEnterMemo() {
        getResultMemo.launch(Intent(this, AddMemoActivity::class.java))
    }

    private val getResultMemo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val memo = it.data?.getStringExtra(AddMemoActivity.EXTRA_MEMO)
                handleMemo(memo)
            }
        }

    private val getResultSendAmount =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                finish()
            }
        }

    private fun gotoSendFundsConfirm() {
        val transfer = viewModel.newTransfer
        val recipient = viewModel.selectedRecipient
        if (transfer != null && recipient != null) {
            val intent = Intent(this, SendFundsConfirmedActivity::class.java)
            intent.putExtra(SendFundsConfirmedActivity.EXTRA_TRANSFER, transfer)
            intent.putExtra(SendFundsConfirmedActivity.EXTRA_RECIPIENT, recipient)
            getResultSendAmount.launch(intent)
        }
    }

    private fun gotoFailed(error: BackendError?) {
        val intent = Intent(this, FailedActivity::class.java)
        intent.putExtra(FailedActivity.EXTRA_SOURCE, FailedViewModel.Source.Transfer)
        error?.let {
            intent.putExtra(FailedActivity.EXTRA_ERROR, it)
        }
        startActivity(intent)
    }

    private fun updateRecipientEditText(text: String){
        //Only update if they are not identical - else it triggers a refocus and moves cursor
        if (binding.sendFundsPasteRecipient.text.toString() != text) {
            binding.sendFundsPasteRecipient.setText(text)
        }
    }

    private fun showRecipient(recipient: Recipient?) {
        if (recipient == null) {
            updateRecipientEditText("")
            binding.confirmButton.setText(R.string.send_funds_confirm)
        } else {
            if(viewModel.isShielded){
                if(viewModel.isTransferToSameAccount()){
                    binding.recipientContainer.visibility = View.GONE
                    binding.confirmButton.setText(R.string.send_funds_confirm_unshield)
                } else {
                    updateRecipientEditText(recipient.address)
                    binding.confirmButton.setText(R.string.send_funds_confirm_send_shielded)
                }
            } else {
                if(viewModel.isTransferToSameAccount()){
                    binding.recipientContainer.visibility = View.GONE
                    binding.confirmButton.setText(R.string.send_funds_confirm_shield)
                } else {
                    updateRecipientEditText(recipient.address)
                    binding.confirmButton.setText(R.string.send_funds_confirm)
                }
            }
        }
    }

    private fun isWaiting(): Boolean {
        var waiting = false
        viewModel.waitingLiveData.value?.let {
            if(it){
                waiting = true
            }
        }
        viewModel.waitingReceiverAccountPublicKeyLiveData.value?.let {
            if(it){
                waiting = true
            }
        }
        return waiting
    }

    private fun updateConfirmButton(): Boolean {
        binding.confirmButton.isEnabled = enableConfirm()
        return binding.confirmButton.isEnabled
    }

    private fun enableConfirm(): Boolean {
        val hasSufficientFunds = viewModel.hasSufficientFunds(binding.amountEdittext.text.toString())
        binding.errorTextview.visibility = if (hasSufficientFunds) View.INVISIBLE else View.VISIBLE
        val enable = if(isWaiting()) false else {
            (binding.amountEdittext.text.isNotEmpty()
                    && viewModel.selectedRecipient != null
                    && viewModel.transactionFeeLiveData.value != null
                    && hasSufficientFunds
                    && (CurrencyUtil.toGTUValue(binding.amountEdittext.text.toString()) ?: BigDecimal.ZERO).signum() > 0)
        }
        return enable
    }

    private fun updateAmountEditText() {
        if (binding.amountEdittext.text.isNotEmpty()) {
            // Only setting this (to one char) to have the width being smaller
            // Width is WRAP_CONTENT and hint text count towards this
            binding.amountEdittext.hint = "0"
            binding.amountEdittext.gravity = Gravity.CENTER
        } else {
            binding.amountEdittext.hint = "0${DecimalFormatSymbols.getInstance().decimalSeparator}00"
            binding.amountEdittext.gravity = Gravity.NO_GRAVITY
        }
    }

    private fun createConfirmString(): String? {
        val amount = viewModel.getAmount()
        val cost = viewModel.transactionFeeLiveData.value
        val recipient = viewModel.selectedRecipient
        if (amount == null || cost == null || recipient == null) {
            showError(R.string.app_error_general)
            return null
        }
        val amountString = CurrencyUtil.formatGTU(amount, withGStroke = true)
        val costString = CurrencyUtil.formatGTU(cost, withGStroke = true)

        val memoText = if(viewModel.getClearTextMemo().isNullOrEmpty()) "" else getString(R.string.send_funds_confirmation_memo, viewModel.getClearTextMemo())

        return if(viewModel.isShielded){
            if(viewModel.isTransferToSameAccount()){
                getString(R.string.send_funds_confirmation_unshield, amountString, recipient.displayName(), costString, memoText)
            } else {
                getString(R.string.send_funds_confirmation_send_shielded, amountString, recipient.displayName(), costString, memoText)
            }
        } else {
            if(viewModel.isTransferToSameAccount()){
                getString(R.string.send_funds_confirmation_shield, amountString, recipient.displayName(), costString, memoText)
            } else {
                getString(R.string.send_funds_confirmation, amountString, recipient.displayName(), costString, memoText)
            }
        }
    }
}
