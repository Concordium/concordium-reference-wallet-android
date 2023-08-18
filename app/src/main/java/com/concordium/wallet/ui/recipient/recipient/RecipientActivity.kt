package com.concordium.wallet.ui.recipient.recipient

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.databinding.ActivityRecipientBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil

class RecipientActivity : BaseActivity() {
    companion object {
        const val EXTRA_RECIPIENT = "EXTRA_RECIPIENT"
        const val EXTRA_SELECT_RECIPIENT_MODE = "EXTRA_SELECT_RECIPIENT_MODE"
        const val EXTRA_GOTO_SCAN_QR = "EXTRA_GOTO_SCAN_QR"
    }

    private lateinit var binding: ActivityRecipientBinding
    private lateinit var viewModel: RecipientViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipientBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.recipient_new_title)

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

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[RecipientViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
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
        viewModel.gotoBackToSendLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    goBackWithRecipient(viewModel.recipient)
                }
            }
        })

    }

    private fun initViews() {
        showWaiting(false)
        binding.saveButton.isEnabled = false
        if (viewModel.editRecipientMode) {
            setActionBarTitle(R.string.recipient_edit_title)
        }
        binding.recipientNameEdittext.afterTextChanged {
            binding.saveButton.isEnabled = validateRecipient()
        }
        binding.recipientAddressEdittext.afterTextChanged {
            binding.saveButton.isEnabled = validateRecipient()
        }
        binding.saveButton.setOnClickListener {
            saveRecipient()
        }
        binding.qrImageview.setOnClickListener {
            gotoScanBarCode()
        }
        // Setting these after the text changed listeners have been added above
        // To trigger initial validation and enabling the button
        binding.recipientNameEdittext.setText(viewModel.recipient.name)
        binding.recipientAddressEdittext.setText(viewModel.recipient.address)
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }

    private fun showError(stringRes: Int) {
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(binding.rootLayout, stringRes)
    }

    private fun validateRecipient(): Boolean {
        return viewModel.validateRecipient(
            binding.recipientNameEdittext.text.toString(),
            binding.recipientAddressEdittext.text.toString()
        )
    }

    private fun saveRecipient() {
        val isSaving = viewModel.validateAndSaveRecipient(
            binding.recipientNameEdittext.text.toString(),
            binding.recipientAddressEdittext.text.toString()
        )
        if (isSaving) {
            binding.saveButton.isEnabled = false
            KeyboardUtil.hideKeyboard(this)
        }
    }

    private fun goBackWithRecipient(recipient: Recipient) {
        val intent = Intent()
        intent.putExtra(EXTRA_RECIPIENT, recipient)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private val getResultScanQr =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(ScanQRActivity.EXTRA_BARCODE)?.let { barcode ->
                    showAddress(barcode)
                }
            }
        }

    private fun gotoScanBarCode() {
        val intent = Intent(this, ScanQRActivity::class.java)
        intent.putExtra(ScanQRActivity.QR_MODE, ScanQRActivity.QR_MODE_CONCORDIUM_ACCOUNT)
        getResultScanQr.launch(intent)
    }

    private fun showAddress(address: String) {
        binding.recipientAddressEdittext.setText(address)
    }

    //endregion
}
