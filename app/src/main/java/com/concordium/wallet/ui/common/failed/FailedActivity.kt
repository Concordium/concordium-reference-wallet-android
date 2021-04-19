package com.concordium.wallet.ui.common.failed

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.BackendErrorHandler
import kotlinx.android.synthetic.main.activity_failed.*

class FailedActivity :
    BaseActivity(R.layout.activity_failed, R.string.failed_title) {

    companion object {
        const val EXTRA_ERROR = "EXTRA_ERROR"
        const val EXTRA_SOURCE = "EXTRA_SOURCE"
    }

    private lateinit var viewModel: FailedViewModel


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        ).get(FailedViewModel::class.java)
    }

    private fun initViews() {
        hideActionBarBack(this)

        when (viewModel.source) {
            FailedViewModel.Source.Identity -> {
                setActionBarTitle(R.string.identity_confirmed_title)
                error_title_textview.setText(R.string.identity_confirmed_failed)
            }
            FailedViewModel.Source.Account -> {
                setActionBarTitle(R.string.new_account_confirmed_title)
                error_title_textview.setText(R.string.new_account_confirmed_failed)
            }
            FailedViewModel.Source.Transfer -> {
                setActionBarTitle(R.string.send_funds_title)
                error_title_textview.setText(R.string.send_funds_confirmed_failed)
            }
        }

        viewModel.error?.let { backendError ->
            if(backendError.errorMessage != null){
                error_textview.setText(backendError.errorMessage)
            }
            else
            BackendErrorHandler.getExceptionStringResOrNull(backendError)?.let { stringRes ->
                error_textview.setText(stringRes)
            }
        }

        confirm_button.setOnClickListener {
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
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
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
