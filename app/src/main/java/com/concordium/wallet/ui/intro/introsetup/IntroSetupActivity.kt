package com.concordium.wallet.ui.intro.introsetup

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityIntroSetupBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.passphrase.recover.RecoverWalletActivity
import com.concordium.wallet.ui.passphrase.setup.SetupWalletActivity

class IntroSetupActivity : BaseActivity() {
    private lateinit var binding: ActivityIntroSetupBinding
    private lateinit var viewModel: IntroSetupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.intro_setup_title)
        initializeViewModel()
        viewModel.initialize()
        initViews()
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[IntroSetupViewModel::class.java]
    }

    private fun initViews() {
        hideActionBarBack()
        binding.setupWallet.setOnClickListener {
            gotoSetupWallet()
        }
        binding.recoverWallet.setOnClickListener {
            gotoRecoverWallet()
        }
    }

    private fun gotoSetupWallet() {
        startActivity(Intent(this, SetupWalletActivity::class.java))
    }

    private fun gotoRecoverWallet() {
        startActivity(Intent(this, RecoverWalletActivity::class.java))
    }
}
