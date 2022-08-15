package com.concordium.wallet.ui.account.newaccountconfirmed

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityNewAccountConfirmedBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity

class NewAccountConfirmedActivity : BaseActivity() {
    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    private lateinit var binding: ActivityNewAccountConfirmedBinding
    private lateinit var viewModel: NewAccountConfirmedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewAccountConfirmedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.new_account_confirmed_title)

        val account = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account

        initializeViewModel()
        viewModel.initialize(account)
        // This observe has to be done after the initialize, where the live data is set up in the view model
        viewModel.accountWithIdentityLiveData.observe(this) { accountWithIdentity ->
            accountWithIdentity?.let {
                binding.accountView.setAccount(accountWithIdentity)
            }
        }
        initViews()
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NewAccountConfirmedViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
    }

    private fun initViews() {
        hideActionBarBack(this)
        showWaiting(false)

        binding.confirmButton.setOnClickListener {
            gotoAccountOverview()
        }
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }

    private fun gotoAccountOverview() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}
