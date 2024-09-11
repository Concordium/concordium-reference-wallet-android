package com.concordium.wallet.ui.passphrase.recover

import android.os.Bundle
import android.view.WindowManager
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityExportSeedBinding
import com.concordium.wallet.ui.base.BaseActivity

class ExportSeedActivity : BaseActivity() {
    private lateinit var binding: ActivityExportSeedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportSeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.export_seed_title
        )
    }
}