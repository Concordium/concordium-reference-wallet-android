package com.concordium.wallet.ui.recipient.recipient

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_recipient.*
import kotlinx.android.synthetic.main.progress.*

class RecipientActivity :
    BaseActivity(R.layout.activity_recipient, R.string.recipient_new_title) {

    companion object {
        const val EXTRA_RECIPIENT = "EXTRA_RECIPIENT"
        const val EXTRA_SELECT_RECIPIENT_MODE = "EXTRA_SELECT_RECIPIENT_MODE"
        const val EXTRA_GOTO_SCAN_QR = "EXTRA_GOTO_SCAN_QR"
        const val REQUESTCODE_SCAN_QR = 2000
    }

    private lateinit var viewModel: RecipientViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recipient = intent.getSerializableExtra(EXTRA_RECIPIENT) as Recipient?
        val selectRecipientMode = intent.getBooleanExtra(EXTRA_SELECT_RECIPIENT_MODE, false)

        initializeViewModel()
        viewModel.initialize(recipient, selectRecipientMode)
        initViews()

        val gotoScanQR = intent.getBooleanExtra(EXTRA_GOTO_SCAN_QR, false)
        if (gotoScanQR) {
            gotoScanBarCode()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUESTCODE_SCAN_QR) {
            if (resultCode == Activity.RESULT_OK) {
                data?.getStringExtra(ScanQRActivity.EXTRA_BARCODE)?.let { barcode ->
                    showAddress(barcode)
                }
            }
        }
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(RecipientViewModel::class.java)

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })
        viewModel.errorLiveData.observe(
            this, object : EventObserver<Int>() {
                override fun onUnhandledEvent(value: Int) {
                    showError(value)
                }
            }
        )
        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    finish()
                }
            }
        })
        viewModel.gotoBackToSendFundsLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    goBackToSendFunds(viewModel.recipient)
                }
            }
        })

    }

    private fun initViews() {
        showWaiting(false)
        save_button.isEnabled = false
        if (viewModel.editRecipientMode) {
            setActionBarTitle(R.string.recipient_edit_title)
        }
        recipient_name_edittext.afterTextChanged {
            save_button.isEnabled = validateRecipient()
        }
        recipient_address_edittext.afterTextChanged {
            save_button.isEnabled = validateRecipient()
        }
        save_button.setOnClickListener {
            saveRecipient()
        }
        qr_imageview.setOnClickListener {
            gotoScanBarCode()
        }
        // Setting these after the text changed listeners have been added above
        // To trigger initial validation and enabling the button
        recipient_name_edittext.setText(viewModel.recipient.name)
        recipient_address_edittext.setText(viewModel.recipient.address)
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    private fun showError(stringRes: Int) {
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(root_layout, stringRes)
    }

    private fun validateRecipient(): Boolean {
        return viewModel.validateRecipient(
            recipient_name_edittext.text.toString(),
            recipient_address_edittext.text.toString()
        )
    }

    private fun saveRecipient() {
        val isSaving = viewModel.validateAndSaveRecipient(
            recipient_name_edittext.text.toString(),
            recipient_address_edittext.text.toString()
        )
        if (isSaving) {
            save_button.isEnabled = false
            KeyboardUtil.hideKeyboard(this)
        }

    }

    private fun goBackToSendFunds(recipient: Recipient) {
        val intent = Intent(this, SendFundsActivity::class.java)
        intent.putExtra(SendFundsActivity.EXTRA_RECIPIENT, recipient)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun gotoScanBarCode() {
        val intent = Intent(this, ScanQRActivity::class.java)
        startActivityForResult(intent, REQUESTCODE_SCAN_QR)
    }

    private fun showAddress(address: String) {
        recipient_address_edittext.setText(address)
    }

    //endregion
}
