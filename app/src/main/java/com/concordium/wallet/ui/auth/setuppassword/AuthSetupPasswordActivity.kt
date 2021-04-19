package com.concordium.wallet.ui.auth.setuppassword

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.ui.auth.setup.AuthSetupActivity
import com.concordium.wallet.ui.auth.setupbiometrics.AuthSetupBiometricsActivity
import com.concordium.wallet.ui.auth.setuppasswordrepeat.AuthSetupPasswordRepeatActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_auth_setup_password.*

class AuthSetupPasswordActivity :
    BaseActivity(R.layout.activity_auth_setup_password, R.string.auth_setup_password_title) {

    private val REQUESTCODE_AUTH_SETUP_BIOMETRICS = 2000
    private val REQUESTCODE_AUTH_SETUP_PASSWORD_REPEAT = 2001

    private lateinit var viewModel: AuthSetupPasswordViewModel

    private var continueFlow: Boolean = true


    companion object {
        val CONTINUE_INITIAL_SETUP = "CONTINUE_INITIAL_SETUP"
    }


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        continueFlow = intent.getBooleanExtra(AuthSetupActivity.CONTINUE_INITIAL_SETUP, true)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUESTCODE_AUTH_SETUP_BIOMETRICS) {
            if (resultCode == Activity.RESULT_OK) {
                if(continueFlow){
                    viewModel.hasFinishedSetupPassword()
                }
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
        if (requestCode == REQUESTCODE_AUTH_SETUP_PASSWORD_REPEAT) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.setupPassword(password_edittext.text.toString())
            } else {
                password_edittext.setText("")
                error_textview.setText(R.string.auth_error_entries_different)
            }
        }
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(AuthSetupPasswordViewModel::class.java)

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
        confirm_button.setOnClickListener {
            onConfirmClicked()
        }
        confirm_button.isEnabled = false
        password_edittext.afterTextChanged {
            error_textview.setText("")
            confirm_button.isEnabled =
                viewModel.checkPasswordRequirements(password_edittext.text.toString())
        }
        password_edittext.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (viewModel.checkPasswordRequirements(password_edittext.text.toString())) {
                        onConfirmClicked()
                    }
                    true
                }
                else -> false
            }
        }
        password_edittext.requestFocus()
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun onConfirmClicked() {
        if (viewModel.checkPasswordRequirements(password_edittext.text.toString())) {
            viewModel.startSetupPassword(password_edittext.text.toString())
            gotoAuthSetupPasswordRepeat()
        } else {
            password_edittext.setText("")
            error_textview.setText(R.string.auth_error_password_not_valid)
        }
    }

    private fun gotoAuthSetupBiometrics() {
        val intent = Intent(this, AuthSetupBiometricsActivity::class.java)
        startActivityForResult(intent, REQUESTCODE_AUTH_SETUP_BIOMETRICS)
    }

    private fun gotoAuthSetupPasswordRepeat() {
        val intent = Intent(this, AuthSetupPasswordRepeatActivity::class.java)
        startActivityForResult(intent, REQUESTCODE_AUTH_SETUP_PASSWORD_REPEAT)
    }

    private fun showPasswordError() {
        password_edittext.setText("")
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(root_layout, R.string.auth_error_password_setup)
    }

    //endregion


}
