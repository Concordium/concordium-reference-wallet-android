package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityTokenDetailsBinding
import com.concordium.wallet.ui.base.BaseActivity

class TokenDetailsActivity : BaseActivity() {
    private lateinit var binding: ActivityTokenDetailsBinding
    private lateinit var viewModel: TokensViewModel

    companion object {
        const val TOKEN_NAME = "TOKEN_NAME"
        const val ACCOUNT_NAME = "ACCOUNT_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTokenDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        initializeViewModel()
        initObservers()
        lookForTokensView()
    }

    private fun initViews() {
        val tokenName = intent.extras!!.getString(TOKEN_NAME)
        val accountName = intent.extras!!.getString(ACCOUNT_NAME)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.app_name)
        setActionBarTitle(getString(R.string.cis_token_details_title, tokenName, accountName))
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[TokensViewModel::class.java]
    }

    private fun initObservers() {
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }

    private fun lookForTokensView() {

    }
}
