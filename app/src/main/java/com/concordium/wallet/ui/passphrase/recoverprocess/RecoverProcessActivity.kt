package com.concordium.wallet.ui.passphrase.recoverprocess

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.databinding.ActivityRecoverProcessBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.passphrase.recover.RecoverWalletActivity

class RecoverProcessActivity : BaseActivity(), AuthDelegate by AuthDelegateImpl() {
    private lateinit var binding: ActivityRecoverProcessBinding
    private lateinit var viewModel: RecoverProcessViewModel
    private var passwordSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoverProcessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.pass_phrase_recover_process_title)
        hideActionBarBack(this)
        initializeViewModel()
        initViews()
        initObservers()
    }

    override fun onBackPressed() {
        // Ignore back press
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
        binding.enterAnotherPhraseButton.setOnClickListener {
            AuthPreferences(this).setSeedPhrase("")
            finish()
            startActivity(Intent(this, RecoverWalletActivity::class.java))
        }
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.statusChanged.observe(this) { status ->
            runOnUiThread {
                when (status) {
                    RecoverProcessViewModel.STATUS_OK -> finishScanningView()
                    RecoverProcessViewModel.STATUS_NOTHING_TO_RECOVER -> nothingToRecoverView()
                }
            }
        }
    }

    private fun nothingToRecoverView() {
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, RecoverProcessNothingToRecoverFragment(), null).commit()
        binding.continueButton.visibility = View.GONE
        binding.tryAgainButton.visibility = View.VISIBLE
        binding.enterAnotherPhraseButton.visibility = View.VISIBLE
    }

    private fun finishScanningView() {
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, RecoverProcessFinishedFragment.newInstance(viewModel), null).commit()
        binding.continueButton.visibility = View.VISIBLE
        binding.tryAgainButton.visibility = View.GONE
        binding.enterAnotherPhraseButton.visibility = View.GONE
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
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, RecoverProcessScanningFragment.newInstance(viewModel, password), null).commit()
        binding.continueButton.visibility = View.GONE
        binding.tryAgainButton.visibility = View.GONE
        binding.enterAnotherPhraseButton.visibility = View.GONE
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressLayout.visibility = if (waiting) View.VISIBLE else View.GONE
    }
}
