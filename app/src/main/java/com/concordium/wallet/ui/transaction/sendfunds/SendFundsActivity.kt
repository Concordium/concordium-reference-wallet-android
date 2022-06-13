package com.concordium.wallet.ui.transaction.sendfunds

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.CBORUtil
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.failed.FailedActivity
import com.concordium.wallet.ui.common.failed.FailedViewModel
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.ui.transaction.sendfundsconfirmed.SendFundsConfirmedActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_send_funds.*
import kotlinx.android.synthetic.main.progress.*
import java.text.DecimalFormatSymbols
import javax.crypto.Cipher

class SendFundsActivity :
    BaseActivity(R.layout.activity_send_funds, R.string.send_funds_title) {

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val EXTRA_RECIPIENT = "EXTRA_RECIPIENT"
        const val EXTRA_MEMO = "EXTRA_MEMO"
        const val REQUESTCODE_SCAN_QR = 2000
        const val SEND_AMOUNT = 3000

    }

    private lateinit var viewModel: SendFundsViewModel


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        val isShielded = intent.extras!!.getBoolean(EXTRA_SHIELDED)
        initializeViewModel()
        viewModel.initialize(account, isShielded)
        handleRecipientIntent(intent)
        handleMemo(intent)
        initViews()

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleRecipientIntent(intent)
        handleMemo(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUESTCODE_SCAN_QR) {
            if (resultCode == Activity.RESULT_OK) {
                data?.getStringExtra(ScanQRActivity.EXTRA_BARCODE)?.let { barcode ->
                    handleRecipient(Recipient(0,"", barcode))
                }
            }
        }

        if (requestCode == SEND_AMOUNT) {
            if (resultCode == Activity.RESULT_OK) {
                finish()
            }
        }

    }

    fun handleRecipientIntent(intent: Intent?){
        val recipient = intent?.getSerializableExtra(EXTRA_RECIPIENT) as? Recipient
        handleRecipient(recipient)
    }

    fun handleRecipient(recipient: Recipient?){
        recipient?.let {
            viewModel.selectedRecipient = recipient
            updateConfirmButton()
            if(viewModel.account.id == recipient.id){
                setActionBarTitle(if(viewModel.isShielded) R.string.send_funds_unshield_title else R.string.send_funds_shield_title)
                if (!viewModel.isShielded)
                    send_all.visibility = View.GONE
            }
        }
    }

    fun handleMemo(intent: Intent?){
        intent?.let {
            if(it.hasExtra(EXTRA_MEMO)){
                val memo = it.getStringExtra(EXTRA_MEMO) as? String
                if(memo != null && memo.isNotEmpty()){
                    viewModel.setMemo(CBORUtil.encodeCBOR(memo))
                    setMemoText(memo)
                }
                else{
                    viewModel.setMemo(null)
                    setMemoText("")
                }
            }
        }
    }

    fun setMemoText(txt: String){
        if(txt.isNotEmpty()){
            memo_textview.text = txt
            memo_clear.visibility = View.VISIBLE
        }
        else{
            memo_textview.text = getString(R.string.send_funds_optional_add_memo)
            memo_clear.visibility = View.INVISIBLE
        }
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(SendFundsViewModel::class.java)

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(isWaiting())
            }
        })
        viewModel.waitingReceiverAccountPublicKeyLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(isWaiting())
            }
        })
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(createConfirmString(), viewModel.shouldUseBiometrics(), viewModel.usePasscode(), object : AuthenticationCallback{
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
        viewModel.transactionFeeLiveData.observe(this, object : Observer<Long> {
            override fun onChanged(value: Long?) {
                value?.let {
                    fee_info_textview.visibility = View.VISIBLE
                    fee_info_textview.text = getString(
                        R.string.send_funds_fee_info, CurrencyUtil.formatGTU(value)
                    )
                    updateConfirmButton()
                }
            }
        })
        viewModel.recipientLiveData.observe(this, object : Observer<Recipient> {
            override fun onChanged(value: Recipient?) {
                showRecipient(value)
            }
        })
    }

    private fun initViews() {
        progress_layout.visibility = View.GONE
        error_textview.visibility = View.INVISIBLE
        amount_edittext.afterTextChanged { _ ->
            if (amount_edittext.hasFocus()) { //If it does not have focus, it means we are injecting text programatically
                viewModel.disableSendAllValue()
            }
            updateConfirmButton()
            updateAmountEditText()
        }
        updateAmountEditText()
        amount_edittext.setOnEditorActionListener { textView, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (textView.text.isNotEmpty() && App.appCore.cryptoLibrary.checkAccountAddress(viewModel.selectedRecipient?.address ?: ""))
                        viewModel.sendFunds(amount_edittext.text.toString())
                    else
                        KeyboardUtil.hideKeyboard(this)
                    true
                }
                else -> false
            }
        }
        send_funds_paste_recipient.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                var address = editable.toString()
                if(viewModel.selectedRecipient == null){
                    handleRecipient(Recipient(0,"",address))
                }
                if(!address.equals(viewModel.selectedRecipient?.address)){
                    handleRecipient(Recipient(0,"",address))
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        if(viewModel.isTransferToSameAccount()){
            memo_container.visibility = View.GONE
        }
        else{
            memo_container.visibility = View.VISIBLE

            memo_container.setOnClickListener {
                if(viewModel.showMemoWarning()){
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(getString(R.string.transaction_memo_warning_title))
                    builder.setMessage(getString(R.string.transaction_memo_warning_text))
                    builder.setNegativeButton(getString(R.string.transaction_memo_warning_dont_show), object: DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which:Int) {
                            viewModel.dontShowMemoWarning()
                            gotoEnterMemo()
                        }
                    })
                    builder.setPositiveButton(getString(R.string.transaction_memo_warning_ok), object: DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which:Int) {
                            gotoEnterMemo()
                        }
                    })
                    builder.setCancelable(true)
                    builder.create().show()
                }
                else{
                    gotoEnterMemo()
                }
            }

            setMemoText("")

            memo_clear.setOnClickListener {
                viewModel.setMemo(null);//CBORUtil.encodeCBOR(""))
                setMemoText("")
            }
        }

        send_all.setOnClickListener {
            viewModel.updateSendAllValue()
        }

        viewModel.sendAllAmountLiveData.observe(this, object : Observer<Long> {
            override fun onChanged(value: Long?) {
                value?.let {
                    amount_edittext.setText(CurrencyUtil.formatGTU(value))
                }
            }
        })

        search_recipient_layout.setOnClickListener {
            gotoSelectRecipient()
        }
        scan_qr_recipient_layout.setOnClickListener {
            gotoScanBarCode()
        }

        confirm_button.setOnClickListener {
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
            balance_total_text.text = getString(R.string.accounts_overview_balance_at_disposal)
            at_disposal_total_text.text = getString(R.string.accounts_overview_shielded_balance)
            balance_total_textview.text = CurrencyUtil.formatGTU(viewModel.account.getAtDisposalWithoutStakedOrScheduled(viewModel.account.totalUnshieldedBalance), withGStroke = true)
            at_disposal_total_textview.text = CurrencyUtil.formatGTU(viewModel.account.totalShieldedBalance, withGStroke = true)
        }
        else {
            balance_total_text.text = getString(R.string.accounts_overview_account_total)
            at_disposal_total_text.text = getString(R.string.accounts_overview_at_disposal)
            balance_total_textview.text = CurrencyUtil.formatGTU(viewModel.account.totalUnshieldedBalance, withGStroke = true)
            at_disposal_total_textview.text = CurrencyUtil.formatGTU(viewModel.account.getAtDisposalWithoutStakedOrScheduled(viewModel.account.totalUnshieldedBalance), withGStroke = true)
        }
    }

    private fun check95PercentWarning() {
        val amountValue = CurrencyUtil.toGTUValue(amount_edittext.text.toString())
        val atDisposal = viewModel.account.getAtDisposalWithoutStakedOrScheduled(viewModel.account.totalUnshieldedBalance)
        if (amountValue != null) {
            if (amountValue > atDisposal * 0.95) {
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
        viewModel.sendFunds(amount_edittext.text.toString())
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        progress_layout.visibility = if(waiting) View.VISIBLE else View.GONE
        // Update button enabled state, because it is dependant on waiting state
        updateConfirmButton()
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(root_layout, stringRes)
    }


    private fun gotoSelectRecipient() {
        val intent = Intent(this, RecipientListActivity::class.java)
        intent.putExtra(RecipientListActivity.EXTRA_SELECT_RECIPIENT_MODE, true)
        intent.putExtra(RecipientListActivity.EXTRA_SHIELDED, viewModel.isShielded)
        intent.putExtra(RecipientListActivity.EXTRA_ACCOUNT, viewModel.account)
        startActivity(intent)
    }

    private fun gotoScanBarCode() {
        val intent = Intent(this, ScanQRActivity::class.java)
        startActivityForResult(intent, REQUESTCODE_SCAN_QR)
    }


    private fun gotoEnterMemo() {
        val intent = Intent(this, AddMemoActivity::class.java)
        intent.putExtra(AddMemoActivity.EXTRA_MEMO, viewModel.getClearTextMemo())
        startActivity(intent)
    }

    private fun gotoSendFundsConfirm() {
        val transfer = viewModel.newTransfer
        val recipient = viewModel.selectedRecipient
        if (transfer != null && recipient != null) {
            val intent = Intent(this, SendFundsConfirmedActivity::class.java)
            intent.putExtra(SendFundsConfirmedActivity.EXTRA_TRANSFER, transfer)
            intent.putExtra(SendFundsConfirmedActivity.EXTRA_RECIPIENT, recipient)
            startActivityForResult(intent, SEND_AMOUNT)
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
        if(!send_funds_paste_recipient.text.toString().equals(text)){
            send_funds_paste_recipient.setText(text)
        }
    }

    private fun showRecipient(recipient: Recipient?) {
        if (recipient == null) {
            updateRecipientEditText("")
            confirm_button.setText(R.string.send_funds_confirm)
        } else {
            if(viewModel.isShielded){
                if(viewModel.isTransferToSameAccount()){
                    recipient_container.visibility = View.GONE
                    confirm_button.setText(R.string.send_funds_confirm_unshield)
                } else {
                    updateRecipientEditText(recipient.address)
                    confirm_button.setText(R.string.send_funds_confirm_send_shielded)
                }
            } else {
                if(viewModel.isTransferToSameAccount()){
                    recipient_container.visibility = View.GONE
                    confirm_button.setText(R.string.send_funds_confirm_shield)
                } else {
                    updateRecipientEditText(recipient.address)
                    confirm_button.setText(R.string.send_funds_confirm)
                }
            }
        }
    }

    fun isWaiting(): Boolean {
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
        val hasSufficientFunds = viewModel.hasSufficientFunds(amount_edittext.text.toString())
        error_textview.visibility = if (hasSufficientFunds) View.INVISIBLE else View.VISIBLE
        val enabled = if(isWaiting()) false else {
            (amount_edittext.text.isNotEmpty()
                    && viewModel.selectedRecipient != null
                    && viewModel.transactionFeeLiveData.value != null
                    && hasSufficientFunds
                    && (CurrencyUtil.toGTUValue(amount_edittext.text.toString()) ?: 0) > 0)
        }
        confirm_button.isEnabled = enabled
        return enabled
    }

    private fun updateAmountEditText() {
        if (amount_edittext.text.isNotEmpty()) {
            // Only setting this (to one char) to have the width being smaller
            // Width is WRAP_CONTENT and hint text count towards this
            amount_edittext.setHint("0")
            amount_edittext.gravity = Gravity.CENTER
        } else {
            amount_edittext.setHint("0${DecimalFormatSymbols.getInstance().decimalSeparator}00")
            amount_edittext.gravity = Gravity.NO_GRAVITY
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



    //endregion
}
