package com.concordium.wallet.ui.cis2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityTokenDetailsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.getSerializable

class TokenDetailsActivity : BaseActivity() {
    private lateinit var binding: ActivityTokenDetailsBinding
    private lateinit var viewModel: TokensViewModel

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val TOKEN_NAME = "TOKEN_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTokenDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeViewModel()
        initViews()
        initObservers()
        lookForTokensView()
    }

    private fun initViews() {
        viewModel.account = intent.getSerializable(ACCOUNT, Account::class.java)
        val tokenName = intent.extras!!.getString(TOKEN_NAME)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.app_name)
        setActionBarTitle(getString(R.string.cis_token_details_title, tokenName, viewModel.account.name))
        binding.send.setOnClickListener {
            val intent = Intent(this, SendTokenActivity::class.java)
            intent.putExtra(SendTokenActivity.ACCOUNT, viewModel.account)
            intent.putExtra(SendTokenActivity.TOKEN, Token("default", "default", "DEF", 123))
            startActivity(intent)
        }
        binding.receive.setOnClickListener {

        }
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[TokensViewModel::class.java]
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }

    private fun lookForTokensView() {

    }
}
