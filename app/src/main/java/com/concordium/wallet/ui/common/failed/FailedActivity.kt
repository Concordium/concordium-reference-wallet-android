package com.concordium.wallet.ui.common.failed

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.databinding.ActivityFailedBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.ui.passphrase.recoverprocess.RecoverProcessActivity

class FailedActivity : BaseActivity() {
    companion object {
        const val EXTRA_ERROR = "EXTRA_ERROR"
        const val EXTRA_SOURCE = "EXTRA_SOURCE"
    }

    private lateinit var binding: ActivityFailedBinding
    private lateinit var viewModel: FailedViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFailedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.failed_title)

        val source = intent.extras!!.getSerializable(EXTRA_SOURCE) as FailedViewModel.Source
        val error = intent.extras!!.getSerializable(EXTRA_ERROR) as BackendError?
        initializeViewModel()
        viewModel.initialize(source, error)
        initViews()
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    // endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[FailedViewModel::class.java]
    }

    private fun initViews() {
        hideActionBarBack()

        when (viewModel.source) {
            FailedViewModel.Source.Identity -> {
                setActionBarTitle(R.string.identity_confirmed_title)
                binding.errorTitleTextview.setText(R.string.identity_confirmed_failed)
            }
            FailedViewModel.Source.Account -> {
                setActionBarTitle(R.string.new_account_confirmed_title)
                binding.errorTitleTextview.setText(R.string.new_account_confirmed_failed)
            }
            FailedViewModel.Source.Transfer -> {
                setActionBarTitle(R.string.send_funds_title)
                binding.errorTitleTextview.setText(R.string.send_funds_confirmed_failed)
            }
        }

        viewModel.error?.let { backendError ->
            if (backendError.errorMessage != null) {
                if (viewModel.source == FailedViewModel.Source.Account && backendError.error == 1) {
                    binding.errorTextview.setText(R.string.new_account_confirmed_failed_recover_error_text)
                    binding.infoTextview.setText(R.string.new_account_confirmed_failed_recover_info_text)
                    binding.confirmButton.text = getString(R.string.new_account_confirmed_failed_recover_button)
                } else {
                    binding.errorTextview.setText(backendError.errorMessage)
                }
            }
            else
            BackendErrorHandler.getExceptionStringResOrNull(backendError)?.let { stringRes ->
                binding.errorTextview.setText(stringRes)
            }
        }

        binding.confirmButton.setOnClickListener {
            finishFlow()
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun finishFlow() {
        when (viewModel.source) {
            FailedViewModel.Source.Identity -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.putExtra(MainActivity.EXTRA_SHOW_IDENTITIES, true)
                startActivity(intent)
            }
            FailedViewModel.Source.Account -> {
                val errorCode: Int = viewModel.error?.error ?: -9999
                if (errorCode == 1) {
                    val intent = Intent(this, RecoverProcessActivity::class.java)
                    intent.putExtra(RecoverProcessActivity.SHOW_FOR_FIRST_RECOVERY, false)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
            }
            FailedViewModel.Source.Transfer -> {
                val intent = Intent(this, AccountDetailsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
        }
    }

    //endregion
}
