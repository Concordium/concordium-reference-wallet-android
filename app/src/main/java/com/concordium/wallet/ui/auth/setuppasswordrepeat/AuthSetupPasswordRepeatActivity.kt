package com.concordium.wallet.ui.auth.setuppasswordrepeat

import android.app.Activity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityAuthSetupPasswordBinding
import com.concordium.wallet.ui.base.BaseActivity

class AuthSetupPasswordRepeatActivity : BaseActivity() {
    private lateinit var binding: ActivityAuthSetupPasswordBinding
    private lateinit var viewModel: AuthSetupPasswordRepeatViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthSetupPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.auth_setup_password_repeat_title
        )

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
        )[AuthSetupPasswordRepeatViewModel::class.java]
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
        binding.instructionTextview.setText(R.string.auth_setup_password_repeat_info)
        binding.confirmButton.setOnClickListener {
            onConfirmClicked()
        }
        binding.passwordEdittext.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    onConfirmClicked()
                    true
                }

                else -> false
            }
        }
        binding.passwordEdittext.requestFocus()
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun onConfirmClicked() {
        viewModel.checkPassword(binding.passwordEdittext.text.toString())
    }

    //endregion
}
