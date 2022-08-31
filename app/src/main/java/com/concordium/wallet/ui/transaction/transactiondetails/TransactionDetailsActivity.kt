package com.concordium.wallet.ui.transaction.transactiondetails

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.InputType
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
import com.concordium.wallet.databinding.ActivityTransactionDetailsBinding
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.TransactionViewHelper
import kotlinx.coroutines.launch

class TransactionDetailsActivity : BaseActivity() {
    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_TRANSACTION = "EXTRA_TRANSACTION"
        const val EXTRA_ISSHIELDED = "EXTRA_ISSHIELDED"
    }

    private lateinit var binding: ActivityTransactionDetailsBinding
    private lateinit var viewModel: TransactionDetailsViewModel
    private lateinit var accountUpdater: AccountUpdater

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.transaction_details_title)

        val account = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        val transaction = intent.extras!!.getSerializable(EXTRA_TRANSACTION) as Transaction
        val isShielded = intent.extras!!.getBoolean(EXTRA_ISSHIELDED)
        initializeViewModel()
        accountUpdater = AccountUpdater(this.application, viewModel.viewModelScope)
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
        )[TransactionDetailsViewModel::class.java]
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
        binding.contentLayout.visibility = View.GONE
        binding.messageLayout.visibility = View.GONE
        binding.fromAddressLayout.visibility = View.GONE
        binding.toAddressLayout.visibility = View.GONE
        binding.transactionHashLayout.visibility = View.GONE
        binding.blockHashLayout.visibility = View.GONE
        binding.detailsLayout.visibility = View.GONE

        val onClickListener = object : TransactionDetailsEntryView.OnCopyClickListener {
            override fun onCopyClicked(title: String, value: String) {
                val clipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(title, value)
                clipboard.setPrimaryClip(clip)
                popup.showSnackbar(binding.rootLayout, getString(R.string.transaction_details_value_copied, title))
            }
        }
        binding.fromAddressLayout.enableCopy(onClickListener)
        binding.toAddressLayout.enableCopy(onClickListener)
        binding.transactionHashLayout.enableCopy(onClickListener)
        binding.blockHashLayout.enableCopy(onClickListener)
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
        popup.showSnackbar(binding.rootLayout, stringRes)
    }

    private fun showTransactionDetails() {
        binding.contentLayout.visibility = View.VISIBLE
        showDetailsTop()
        showDetailsContent()
    }

    private fun showDetailsTop() {
        val ta = viewModel.transaction

        viewModel.viewModelScope.launch {
            TransactionViewHelper.show(
                accountUpdater,
                ta,
                binding.transaction.titleTextview,
                binding.transaction.subheaderTextview,
                binding.transaction.totalTextview,
                binding.transaction.costTextview,
                binding.transaction.memoTextview,
                binding.transaction.amountTextview,
                binding.transaction.alertImageview,
                binding.transaction.statusImageview,
                binding.transaction.lockImageview,
                viewModel.isShieldedAccount,
                showDate = true
            )
        }

        binding.transaction.titleTextview.isElegantTextHeight = true
        binding.transaction.titleTextview.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
        binding.transaction.titleTextview.isSingleLine = false

        binding.messageTextview.isElegantTextHeight = true
        binding.messageTextview.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
        binding.messageTextview.isSingleLine = false
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
            binding.messageLayout.visibility = View.VISIBLE
            binding.messageTextview.text = ta.rejectReason
        } else {
            binding.messageLayout.visibility = View.GONE
        }
    }

    private fun showFromAddressOrOrigin(ta: Transaction) {
        binding.fromAddressLayout.visibility = View.GONE
        if (ta.fromAddress != null) {
            binding.fromAddressLayout.visibility = View.VISIBLE
            binding.fromAddressLayout.setTitle(
                getString(
                    R.string.transaction_details_from_address,
                    viewModel.addressLookup(ta.fromAddress, ta.fromAddressTitle)
                )
            )
            binding.fromAddressLayout.setValue(ta.fromAddress, true)
        } else {
            val origin = ta.origin
            val type = origin?.type
            val address = origin?.address
            if (origin != null && type != null && address != null) {
                if (type == TransactionOriginType.Account) {
                    binding.fromAddressLayout.visibility = View.VISIBLE
                    binding.fromAddressLayout.setTitle(
                        getString(
                            R.string.transaction_details_origin,
                            viewModel.addressLookup(ta.fromAddress, ta.fromAddressTitle)
                        )
                    )
                    binding.fromAddressLayout.setValue(origin.address, true)
                }
            }
        }
    }

    private fun showToAddress(ta: Transaction) {
        if (ta.toAddress != null) {
            binding.toAddressLayout.visibility = View.VISIBLE
            binding.toAddressLayout.setTitle(
                getString(
                    R.string.transaction_details_to_address,
                    viewModel.addressLookup(ta.toAddress, ta.toAddressTitle)
                )
            )
            binding.toAddressLayout.setValue(ta.toAddress, true)
        } else {
            binding.toAddressLayout.visibility = View.GONE
        }
    }

    private fun showTransactionHash(ta: Transaction) {
        val transactionHash = ta.transactionHash
        if (transactionHash != null) {
            binding.transactionHashLayout.visibility = View.VISIBLE
            binding.transactionHashLayout.setValue(transactionHash, true)
        } else {
            binding.transactionHashLayout.visibility = View.GONE
        }
    }

    private fun showBlockHashes(ta: Transaction) {
        if (ta.transactionStatus == TransactionStatus.RECEIVED) {
            binding.blockHashLayout.visibility = View.VISIBLE
            binding.blockHashLayout.setValue(getString(R.string.transaction_details_block_hash_submitted), true)
        } else if (ta.transactionStatus == TransactionStatus.ABSENT) {
            binding.blockHashLayout.visibility = View.VISIBLE
            binding.blockHashLayout.setValue(getString(R.string.transaction_details_block_hash_failed), true)
        } else {
            val blockHashes = ta.blockHashes
            if (blockHashes != null && blockHashes.isNotEmpty()) {
                binding.blockHashLayout.visibility = View.VISIBLE
                val blockHashesString = StringBuilder("")
                var isFirst = true
                for (blockHash in blockHashes) {
                    if (!isFirst) {
                        blockHashesString.append("\n\n")
                    }
                    blockHashesString.append(blockHash)
                    isFirst = false
                }
                binding.blockHashLayout.setValue(blockHashesString.toString(), true)
            } else {
                binding.blockHashLayout.visibility = View.GONE
            }
        }
    }

    private fun showEvents(ta: Transaction) {
        val events = ta.events
        if (events != null && events.isNotEmpty() &&
            ta.outcome == TransactionOutcome.Success &&
            ta.fromAddress == null && ta.toAddress == null
        ) {
            binding.detailsLayout.visibility = View.VISIBLE
            val eventsString = StringBuilder("")
            var isFirst = true
            for (event in events) {
                if (!isFirst) {
                    eventsString.append("\n\n")
                }
                eventsString.append(event)
                isFirst = false
            }
            binding.detailsLayout.setValue(eventsString.toString())
        } else {
            binding.detailsLayout.visibility = View.GONE
        }
    }

    //endregion
}
