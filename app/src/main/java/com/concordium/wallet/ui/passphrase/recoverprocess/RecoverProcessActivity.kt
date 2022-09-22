package com.concordium.wallet.ui.passphrase.recoverprocess

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityRecoverProcessBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl

class RecoverProcessActivity : BaseActivity(), AuthDelegate by AuthDelegateImpl() {
    private lateinit var binding: ActivityRecoverProcessBinding
    private lateinit var viewModel: RecoverProcessViewModel
    private var passwordSet = false
    private var showForFirstRecovery = true

    companion object {
        const val SHOW_FOR_FIRST_RECOVERY = "SHOW_FOR_FIRST_RECOVERY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoverProcessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showForFirstRecovery = intent.extras?.getBoolean(SHOW_FOR_FIRST_RECOVERY, true) ?: true
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.pass_phrase_recover_process_title)
        if (showForFirstRecovery)
            hideActionBarBack()
        initializeViewModel()
        initViews()
        initObservers()
    }

    override fun onBackPressed() {
        // Ignore back press
        if (!showForFirstRecovery)
            super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stop()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[RecoverProcessViewModel::class.java]
    }

    private fun initViews() {
        initButtons()
        startScanning()
    }

    private fun initButtons() {
        binding.continueButton.setOnClickListener {
            if (passwordSet) {
                finish()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            } else {
                startScanning()
            }
        }
        binding.tryAgainButton.setOnClickListener {
            startScanning()
        }
    }

    private fun initObservers() {
        viewModel.statusChanged.observe(this) {
            runOnUiThread {
                finishScanningView()
            }
        }
        viewModel.errorLiveData.observe(this) {
            showError(it)
        }
    }

    private fun showError(stringRes: Int) {
        runOnUiThread {
            popup.showSnackbar(binding.root, stringRes)
        }
    }

    private fun finishScanningView() {
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, RecoverProcessFinishedFragment.newInstance(viewModel.recoverProcessData), null).commit()
        binding.continueButton.visibility = View.VISIBLE
        if (viewModel.recoverProcessData.noResponseFrom.size > 0) {
            binding.tryAgainButton.visibility = View.VISIBLE
            binding.continueButton.text = getString(R.string.pass_phrase_recover_process_continue)
        } else {
            binding.tryAgainButton.visibility = View.GONE
            binding.continueButton.text = getString(R.string.pass_phrase_recover_process_continue_to_wallet)
        }
    }

    private fun startScanning() {
        showAuthentication(this) { password ->
            password?.let {
                passwordSet = true
                runOnUiThread {
                    scanningView(password)
                }
            }
        }
    }

    private fun scanningView(password: String) {
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, RecoverProcessScanningFragment.newInstance(viewModel, viewModel.recoverProcessData, password), null).commit()
        binding.continueButton.visibility = View.GONE
        binding.tryAgainButton.visibility = View.GONE
    }
}
