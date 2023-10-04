package com.concordium.wallet.ui.passphrase.recover

import android.os.Bundle
import android.view.WindowManager
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityExportSeedPhraseBinding
import com.concordium.wallet.ui.base.BaseActivity

class ExportSeedPhraseActivity : BaseActivity() {
    private lateinit var binding: ActivityExportSeedPhraseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportSeedPhraseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.export_seed_phrase_title
        )

        initView()
    }

    private fun initView() {
        binding.continueButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}