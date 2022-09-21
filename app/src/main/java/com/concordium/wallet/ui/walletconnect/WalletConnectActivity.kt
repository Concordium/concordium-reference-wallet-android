package com.concordium.wallet.ui.walletconnect

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityWalletConnectBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WalletConnectActivity : BaseActivity() {
    private lateinit var binding: ActivityWalletConnectBinding
    private lateinit var viewModel: WalletConnectViewModel
    private var fromDeepLink = true

    companion object {
        const val FROM_DEEP_LINK = "FROM_DEEP_LINK"
        const val WC_URI = "WC_URI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fromDeepLink = intent.extras?.getBoolean(FROM_DEEP_LINK, true) ?: true

        initViews()
        initializeViewModel()
        initObservers()

        if (fromDeepLink) {
            intent?.data?.let {
                viewModel.walletConnectData.wcUri = it.toString()
                println("LC -> From DeepLink = $it")
            }
        } else {
            viewModel.walletConnectData.wcUri = intent?.getStringExtra(WC_URI) ?: ""
            println("LC -> From Camera = ${viewModel.walletConnectData.wcUri}")
        }

        accountsView()
    }

    override fun onBackPressed() {
        if (!fromDeepLink) {
            super.onBackPressed()
        }
        else {
            CoroutineScope(Dispatchers.IO).launch {
                if (viewModel.hasAccounts()) {
                    finish()
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                } else {
                    finish()
                }
            }
        }
    }

    private fun initViews() {
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.wallet_connect_accounts_title)
        if (fromDeepLink)
            hideActionBarBack()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[WalletConnectViewModel::class.java]
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.chooseAccount.observe(this) { accountWithIdentity ->
            viewModel.walletConnectData.account = accountWithIdentity.account
            pairView()
        }
        viewModel.connect.observe(this) {
            approveView()
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }

    private fun accountsView() {
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, WalletConnectChooseAccountFragment.newInstance(viewModel, viewModel.walletConnectData), null).commit()
    }

    private fun pairView() {
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, WalletConnectPairFragment.newInstance(viewModel, viewModel.walletConnectData), null).commit()
    }

    private fun approveView() {
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, WalletConnectApproveFragment.newInstance(viewModel, viewModel.walletConnectData), null).commit()
    }
}
