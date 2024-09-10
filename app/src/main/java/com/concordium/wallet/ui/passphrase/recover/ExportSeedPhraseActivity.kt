package com.concordium.wallet.ui.passphrase.recover

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityExportSeedPhraseBinding
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ExportSeedPhraseActivity : BaseActivity() {
    private lateinit var binding: ActivityExportSeedPhraseBinding
    private val viewModel: ExportPassPhraseViewModel by viewModel()

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
        observeState()
    }

    private fun initView() {
        binding.continueButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is ExportSeedPhraseState.Error -> {
                        binding.continueButton.visibility = View.GONE
                    }

                    is ExportSeedPhraseState.Success -> {
                        binding.continueButton.visibility = View.VISIBLE
                    }

                    else -> Unit
                }
            }
        }
    }
}