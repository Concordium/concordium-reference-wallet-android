package com.concordium.wallet.ui.transaction.transactiondetails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Resources
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOriginType
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.TransactionViewHelper
import kotlinx.android.synthetic.main.activity_transaction_details.*
import kotlinx.android.synthetic.main.item_transaction.*
import kotlinx.android.synthetic.main.progress.*
import kotlinx.coroutines.launch

class TransactionDetailsActivity :
    BaseActivity(R.layout.activity_transaction_details, R.string.transaction_details_title) {

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_TRANSACTION = "EXTRA_TRANSACTION"
        const val EXTRA_ISSHIELDED = "EXTRA_ISSHIELDED"
    }

    private lateinit var viewModel: TransactionDetailsViewModel
    private lateinit var accountUpdater: AccountUpdater

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val account = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        val transaction = intent.extras!!.getSerializable(EXTRA_TRANSACTION) as Transaction
        val isShielded = intent.extras!!.getBoolean(EXTRA_ISSHIELDED) as Boolean
        initializeViewModel()
        accountUpdater = AccountUpdater(this!!.application, viewModel.viewModelScope)
        viewModel.setIsShieldedAccount(isShielded)
        viewModel.initialize(account, transaction)
        initViews()
        viewModel.showData()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(TransactionDetailsViewModel::class.java)

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })
        viewModel.errorLiveData.observe(
            this,
            object : EventObserver<Int>() {
                override fun onUnhandledEvent(value: Int) {
                    showError(value)
                }
            }
        )
        viewModel.showDetailsLiveData.observe(
            this,
            object : EventObserver<Boolean>() {
                override fun onUnhandledEvent(value: Boolean) {
                    showTransactionDetails()
                }
            }
        )
    }

    private fun initViews() {
        content_layout.visibility = View.GONE
        message_layout.visibility = View.GONE
        from_address_layout.visibility = View.GONE
        to_address_layout.visibility = View.GONE
        transaction_hash_layout.visibility = View.GONE
        block_hash_layout.visibility = View.GONE
        details_layout.visibility = View.GONE

        val onClickListener = object : TransactionDetailsEntryView.OnCopyClickListener {
            override fun onCopyClicked(title: String, value: String) {
                val clipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(title, value)
                clipboard.setPrimaryClip(clip)
                popup.showSnackbar(root_layout, getString(R.string.transaction_details_value_copied, title))
            }
        }
        from_address_layout.enableCopy(onClickListener)
        to_address_layout.enableCopy(onClickListener)
        transaction_hash_layout.enableCopy(onClickListener)
        block_hash_layout.enableCopy(onClickListener)
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
        popup.showSnackbar(root_layout, stringRes)
    }

    private fun showTransactionDetails() {
        content_layout.visibility = View.VISIBLE
        showDetailsTop()
        showDetailsContent()
    }

    private fun showDetailsTop() {
        val ta = viewModel.transaction

        viewModel.viewModelScope.launch {
            TransactionViewHelper.show(
                accountUpdater,
                ta,
                title_textview,
                subheader_textview,
                total_textview,
                cost_textview,
                memo_textview,
                amount_textview,
                alert_imageview,
                status_imageview,
                progress_imageview,
                lock_imageview,
                viewModel.isShieldedAccount,
                showDate = true
            )
        }

        memo_textview.setElegantTextHeight(true);
        memo_textview.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        memo_textview.setSingleLine(false);

    }

    private fun showDetailsContent() {
        val ta = viewModel.transaction

        showRejectReason(ta)
        showFromAddressOrOrigin(ta)
        showToAddress(ta)
        showTransactionHash(ta)
        showBlockHashes(ta)
        showEvents(ta)
    }

    private fun showRejectReason(ta: Transaction) {
        if (ta.rejectReason != null) {
            message_layout.visibility = View.VISIBLE
            message_textview.text = ta.rejectReason
        } else {
            message_layout.visibility = View.GONE
        }
    }

    private fun showFromAddressOrOrigin(ta: Transaction) {
        from_address_layout.visibility = View.GONE
        if (ta.fromAddress != null) {
            from_address_layout.visibility = View.VISIBLE
            from_address_layout.setTitle(
                getString(
                    R.string.transaction_details_from_address,
                    ta.fromAddressTitle
                )
            )
            from_address_layout.setValue(ta.fromAddress, true)
        } else {
            val origin = ta.origin
            val type = origin?.type
            val address = origin?.address
            if (origin != null && type != null && address != null) {
                if (type == TransactionOriginType.Account) {
                    from_address_layout.visibility = View.VISIBLE
                    from_address_layout.setTitle(
                        getString(
                            R.string.transaction_details_origin,
                            ta.fromAddressTitle
                        )
                    )
                    from_address_layout.setValue(origin.address, true)
                }
            }

        }
    }

    private fun showToAddress(ta: Transaction) {
        if (ta.toAddress != null) {
            to_address_layout.visibility = View.VISIBLE
            to_address_layout.setTitle(
                getString(
                    R.string.transaction_details_to_address,
                    ta.toAddressTitle
                )
            )
            to_address_layout.setValue(ta.toAddress, true)
        } else {
            to_address_layout.visibility = View.GONE
        }
    }

    private fun showTransactionHash(ta: Transaction) {
        val transactionHash = ta.transactionHash
        if (transactionHash != null) {
            transaction_hash_layout.visibility = View.VISIBLE
            transaction_hash_layout.setValue(transactionHash, true)
        } else {
            transaction_hash_layout.visibility = View.GONE
        }
    }

    private fun showBlockHashes(ta: Transaction) {
        if (ta.transactionStatus == TransactionStatus.RECEIVED) {
            block_hash_layout.visibility = View.VISIBLE
            block_hash_layout.setValue(getString(R.string.transaction_details_block_hash_submitted), true)
        } else if (ta.transactionStatus == TransactionStatus.ABSENT) {
            block_hash_layout.visibility = View.VISIBLE
            block_hash_layout.setValue(getString(R.string.transaction_details_block_hash_failed), true)
        } else {
            val blockHashes = ta.blockHashes
            if (blockHashes != null && blockHashes.isNotEmpty()) {
                block_hash_layout.visibility = View.VISIBLE
                val blockHashesString = StringBuilder("")
                var isFirst = true
                for (blockHash in blockHashes) {
                    if (!isFirst) {
                        blockHashesString.append("\n\n")
                    }
                    blockHashesString.append(blockHash)
                    isFirst = false
                }
                block_hash_layout.setValue(blockHashesString.toString(), true)
            } else {
                block_hash_layout.visibility = View.GONE
            }
        }
    }

    private fun showEvents(ta: Transaction) {
        val events = ta.events
        if (events != null && events.isNotEmpty() &&
            ta.outcome == TransactionOutcome.Success &&
            ta.fromAddress == null && ta.toAddress == null
        ) {
            details_layout.visibility = View.VISIBLE
            val eventsString = StringBuilder("")
            var isFirst = true
            for (event in events) {
                if (!isFirst) {
                    eventsString.append("\n\n")
                }
                eventsString.append(event)
                isFirst = false
            }
            details_layout.setValue(eventsString.toString())
        } else {
            details_layout.visibility = View.GONE
        }
    }

    //endregion


}
