package com.concordium.wallet.ui.intro.introstart

import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityWalletNotSetupBinding
import com.concordium.wallet.ui.base.BaseActivity

class WalletNotSetupActivity : BaseActivity() {
    private lateinit var binding: ActivityWalletNotSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletNotSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    private fun initViews() {
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.wallet_connect_wallet_setup_title)
        hideActionBarBack()
        binding.complete.setOnClickListener {
            finish()
        }
    }

    override fun loggedOut() {
    }
}
