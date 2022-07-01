package com.concordium.wallet.ui.auth.setuprepeat

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityAuthSetupBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.view.PasscodeView

class AuthSetupRepeatActivity : BaseActivity() {
    private lateinit var binding: ActivityAuthSetupBinding
    private lateinit var viewModel: AuthSetupRepeatViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.auth_setup_repeat_title)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AuthSetupRepeatViewModel::class.java]
        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    setResult(Activity.RESULT_OK)
                }
                finish()
            }
        })
    }

    private fun initializeViews() {
        binding.instructionTextview.setText(R.string.auth_setup_repeat_info)
        binding.passcodeView.passcodeListener = object : PasscodeView.PasscodeListener {
            override fun onInputChanged() {
            }

            override fun onDone() {
                onConfirmClicked()
            }
        }
        binding.fullPasswordButton.visibility = View.GONE
        binding.passcodeView.requestFocus()
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun onConfirmClicked() {
        viewModel.checkPassword(binding.passcodeView.getPasscode())
    }

    //endregion
}
