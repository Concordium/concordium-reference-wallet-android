package com.concordium.wallet.ui.account.accountdetails

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.Schedule
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.AccountReleaseScheduleItemBinding
import com.concordium.wallet.databinding.AccountReleaseScheduleTransactionItemBinding
import com.concordium.wallet.databinding.ActivityAccountReleaseScheduleBinding
import com.concordium.wallet.ui.base.BaseActivity
import java.text.DateFormat
import java.util.*

class AccountReleaseScheduleActivity : BaseActivity() {
    private lateinit var binding: ActivityAccountReleaseScheduleBinding
    private lateinit var viewModel: AccountReleaseScheduleViewModel

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
    }

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountReleaseScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.account_release_schedule)

        val account = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        val isShielded = intent.extras!!.getBoolean(EXTRA_SHIELDED)
        initializeViewModel()
        viewModel.initialize(account, isShielded)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        viewModel.populateScheduledReleaseList()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return true
    }


    // endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AccountReleaseScheduleViewModel::class.java]

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModel.finishLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                finish()
            }
        })
        viewModel.scheduledReleasesLiveData.observe(this, Observer<List<Schedule>> { list ->

            binding.accountReleaseScheduleLockedAmount.text = CurrencyUtil.formatGTU(viewModel.account.finalizedAccountReleaseSchedule?.total?.toLong() ?: 0, true)

            binding.accountReleaseScheduleList.removeAllViews()
            val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
            list.forEach { release ->
                val view = AccountReleaseScheduleItemBinding.inflate(LayoutInflater.from(this))
                view.date.text = dateFormat.format(Date(release.timestamp))

                release.transactions.forEach { transaction ->
                    val viewTransaction = AccountReleaseScheduleTransactionItemBinding.inflate(LayoutInflater.from(this))
                    viewTransaction.identifier.text = transaction.subSequence(0,8)
                    view.identifierContainer.addView(viewTransaction.root)
                    viewTransaction.copy.tag = transaction
                    viewTransaction.copy.setOnClickListener {
                        val clipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(getString(R.string.account_release_schedule_copy_title),
                            it.tag.toString()
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, getString(R.string.account_release_schedule_copied), Toast.LENGTH_SHORT).show()
                    }
                }

                view.amount.setText(CurrencyUtil.formatGTU(release.amount.toLong(), true))
                binding.accountReleaseScheduleList.addView(view.root)
            }
        })
    }

    private fun initViews() {
        setActionBarTitle(binding.toolbarLayout.toolbarSubTitle, viewModel.account.getAccountName(), getString(R.string.account_release_schedule))
        showWaiting(false)
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

    //endregion
}
