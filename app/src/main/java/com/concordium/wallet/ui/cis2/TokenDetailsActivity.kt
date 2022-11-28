package com.concordium.wallet.ui.cis2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityTokenDetailsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.getSerializable

class TokenDetailsActivity : BaseActivity() {
    private lateinit var binding: ActivityTokenDetailsBinding
    private val viewModel: TokensViewModel by viewModels()

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val TOKEN = "TOKEN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTokenDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        initObservers()
        lookForTokensView()
    }

    private fun initViews() {
        viewModel.tokenData.account = intent.getSerializable(ACCOUNT, Account::class.java)
        viewModel.tokenData.selectedToken = intent.getSerializable(TOKEN, Token::class.java)
        val tokenName = viewModel.tokenData.selectedToken?.token
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.app_name)
        setActionBarTitle(getString(R.string.cis_token_details_title, tokenName, viewModel.tokenData.account?.name))
        binding.includeButtons.send.setOnClickListener {
            val intent = Intent(this, SendTokenActivity::class.java)
            intent.putExtra(SendTokenActivity.ACCOUNT, viewModel.tokenData.account)
            intent.putExtra(SendTokenActivity.TOKEN, Token("0", "default","0", null, false, ""))
            startActivity(intent)
        }
        binding.includeButtons.receive.setOnClickListener {

        }
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