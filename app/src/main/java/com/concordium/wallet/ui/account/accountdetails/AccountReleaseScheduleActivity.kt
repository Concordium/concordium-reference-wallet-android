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
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.account_release_schedule_item.view.*
import kotlinx.android.synthetic.main.account_release_schedule_item.view.identifier_container
import kotlinx.android.synthetic.main.account_release_schedule_transaction_item.view.*
import kotlinx.android.synthetic.main.activity_account_details.*
import kotlinx.android.synthetic.main.activity_account_details.root_layout
import kotlinx.android.synthetic.main.activity_account_release_schedule.*
import kotlinx.android.synthetic.main.progress.*
import java.text.DateFormat
import java.util.*


class AccountReleaseScheduleActivity :
    BaseActivity(R.layout.activity_account_release_schedule, R.string.account_release_schedule) {

    private lateinit var viewModel: AccountReleaseScheduleViewModel

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
    }


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        ).get(AccountReleaseScheduleViewModel::class.java)

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

            account_release_schedule_locked_amount.text = CurrencyUtil.formatGTU(viewModel.account.finalizedAccountReleaseSchedule?.total?.toLong()?:0, true)

            account_release_schedule_list.removeAllViews();
            val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
            list.forEach { release ->
                val view = LayoutInflater.from(this).inflate(R.layout.account_release_schedule_item, null)
                view.date.setText(dateFormat.format(Date(release.timestamp)))

                release.transactions.forEach { transaction ->
                    val viewTransaction = LayoutInflater.from(this).inflate(R.layout.account_release_schedule_transaction_item, null)
                    viewTransaction.identifier.setText(transaction.subSequence(0,8))
                    view.identifier_container.addView(viewTransaction)
                    view.copy.tag = transaction
                    view.copy.setOnClickListener {
                        val clipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(getString(R.string.account_release_schedule_copy_title),
                            it.tag.toString()
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, getString(R.string.account_release_schedule_copied), Toast.LENGTH_SHORT).show()
                    }
                }

                view.amount.setText(CurrencyUtil.formatGTU(release.amount.toLong(), true))
                account_release_schedule_list.addView(view);
            }
        })


    }

    private fun initViews() {
        setActionBarTitle(viewModel.account.getAccountName(), getString(R.string.account_release_schedule))
        showWaiting(false)
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

    private fun showTotalBalance(totalBalance: Long) {
        balance_textview.text = CurrencyUtil.formatGTU(totalBalance)
    }

    //endregion
}

