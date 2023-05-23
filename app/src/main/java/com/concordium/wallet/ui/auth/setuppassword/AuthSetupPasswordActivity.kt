package com.concordium.wallet.ui.auth.setuppassword

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityAuthSetupPasswordBinding
import com.concordium.wallet.ui.auth.setup.AuthSetupActivity
import com.concordium.wallet.ui.auth.setupbiometrics.AuthSetupBiometricsActivity
import com.concordium.wallet.ui.auth.setuppasswordrepeat.AuthSetupPasswordRepeatActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil

class AuthSetupPasswordActivity : BaseActivity() {
    private lateinit var binding: ActivityAuthSetupPasswordBinding
    private lateinit var viewModel: AuthSetupPasswordViewModel

    private var continueFlow: Boolean = true

    companion object {
        val CONTINUE_INITIAL_SETUP = "CONTINUE_INITIAL_SETUP"
    }

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthSetupPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.auth_setup_password_title
        )

        continueFlow = intent.getBooleanExtra(AuthSetupActivity.CONTINUE_INITIAL_SETUP, true)

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
        )[AuthSetupPasswordViewModel::class.java]
        viewModel.errorLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showPasswordError()
                }
            }
        })
        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        })
        viewModel.gotoBiometricsSetupLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    gotoAuthSetupBiometrics()
                }
            }
        })
    }

    private fun initializeViews() {
        binding.confirmButton.setOnClickListener {
            onConfirmClicked()
        }
        binding.confirmButton.isEnabled = false
        binding.passwordEdittext.afterTextChanged {
            binding.errorTextview.text = ""
            binding.confirmButton.isEnabled =
                viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())
        }
        binding.passwordEdittext.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())) {
                        onConfirmClicked()
                    }
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
        if (viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())) {
            viewModel.startSetupPassword(binding.passwordEdittext.text.toString())
            gotoAuthSetupPasswordRepeat()
        } else {
            binding.passwordEdittext.setText("")
            binding.errorTextview.setText(R.string.auth_error_password_not_valid)
        }
    }

    private val getResultAuthSetupBiometrics =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                if (continueFlow) {
                    viewModel.hasFinishedSetupPassword()
                }
                setResult(Activity.RESULT_OK)
                finish()
            }
        }

    private fun gotoAuthSetupBiometrics() {
        val intent = Intent(this, AuthSetupBiometricsActivity::class.java)
        getResultAuthSetupBiometrics.launch(intent)
    }

    private val getResultAuthSetupPasswordRepeat =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.setupPassword(binding.passwordEdittext.text.toString())
            } else {
                binding.passwordEdittext.setText("")
                binding.errorTextview.setText(R.string.auth_error_entries_different)
            }
        }

    private fun gotoAuthSetupPasswordRepeat() {
        val intent = Intent(this, AuthSetupPasswordRepeatActivity::class.java)
        getResultAuthSetupPasswordRepeat.launch(intent)
    }

    private fun showPasswordError() {
        binding.passwordEdittext.setText("")
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(binding.rootLayout, R.string.auth_error_password_setup)
    }

    //endregion
}
