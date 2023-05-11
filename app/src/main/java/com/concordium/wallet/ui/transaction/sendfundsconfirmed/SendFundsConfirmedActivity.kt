package com.concordium.wallet.ui.transaction.sendfundsconfirmed

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.CBORUtil
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivitySendFundsConfirmedBinding
import com.concordium.wallet.ui.base.BaseActivity

class SendFundsConfirmedActivity : BaseActivity() {
    companion object {
        const val EXTRA_TRANSFER = "EXTRA_TRANSFER"
        const val EXTRA_RECIPIENT = "EXTRA_RECIPIENT"
    }

    private lateinit var binding: ActivitySendFundsConfirmedBinding
    private lateinit var viewModel: SendFundsConfirmedViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendFundsConfirmedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.send_funds_confirmed_title
        )

        val transfer = intent.extras!!.getSerializable(EXTRA_TRANSFER) as Transfer
        val recipient = intent.extras!!.getSerializable(EXTRA_RECIPIENT) as Recipient

        initializeViewModel()
        viewModel.initialize(transfer, recipient)
        initViews()
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    // endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[SendFundsConfirmedViewModel::class.java]
    }

    private fun initViews() {
        hideActionBarBack()

        binding.amountTextview.text =
            CurrencyUtil.formatGTU(viewModel.transfer.amount, withGStroke = true)
        binding.feeTextview.text =
            getString(
                R.string.send_funds_confirmed_fee_info,
                CurrencyUtil.formatGTU(viewModel.transfer.cost)
            )

        if (viewModel.transfer.memo.isNullOrEmpty()) {
            binding.memoConfirmationTextview.visibility = View.GONE
        } else {
            binding.memoConfirmationTextview.visibility = View.VISIBLE
            binding.memoConfirmationTextview.text = getString(
                R.string.send_funds_confirmation_memo,
                viewModel.transfer.memo?.let { CBORUtil.decodeHexAndCBOR(it) } ?: ""
            )
        }

        binding.recipientTextview.text = viewModel.recipient.name.ifEmpty { "" }
        binding.addressTextview.text = viewModel.transfer.toAddress

        binding.confirmButton.setOnClickListener {
            gotoAccountDetails()
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoAccountDetails() {
        val returnIntent = Intent()
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    //endregion
}
