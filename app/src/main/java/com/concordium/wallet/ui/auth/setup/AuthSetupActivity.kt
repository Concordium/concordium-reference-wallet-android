package com.concordium.wallet.ui.auth.setup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.ui.auth.setupbiometrics.AuthSetupBiometricsActivity
import com.concordium.wallet.ui.auth.setuppassword.AuthSetupPasswordActivity
import com.concordium.wallet.ui.auth.setuprepeat.AuthSetupRepeatActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.intro.introsetup.IntroSetupActivity
import com.concordium.wallet.uicore.view.PasscodeView
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_auth_setup.*

class AuthSetupActivity : BaseActivity(R.layout.activity_auth_setup, R.string.auth_setup_title) {

    private var continueFlow: Boolean = true

    private val REQUESTCODE_AUTH_SETUP_BIOMETRICS = 2000
    private val REQUESTCODE_AUTH_SETUP_PASSCODE_REPEAT = 2001
    private val REQUESTCODE_AUTH_SETUP_FULL_PASSWORD = 2002

    companion object {
        val CONTINUE_INITIAL_SETUP = "CONTINUE_INITIAL_SETUP"
    }

    private lateinit var viewModel: AuthSetupViewModel


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        continueFlow = intent.getBooleanExtra(CONTINUE_INITIAL_SETUP, true)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUESTCODE_AUTH_SETUP_BIOMETRICS) {
            if (resultCode == Activity.RESULT_OK) {
                if(continueFlow) {
                    viewModel.hasFinishedSetupPassword()
                }
                finishSuccess()
            }
        }
        if (requestCode == REQUESTCODE_AUTH_SETUP_PASSCODE_REPEAT) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.setupPassword(passcode_view.getPasscode(), continueFlow)
            } else {
                passcode_view.clearPasscode()
                error_textview.setText(R.string.auth_error_entries_different)
            }
        }
        if (requestCode == REQUESTCODE_AUTH_SETUP_FULL_PASSWORD) {
            if (resultCode == Activity.RESULT_OK) {
                if(continueFlow) {
                    viewModel.hasFinishedSetupPassword()
                }
                finishSuccess()
            }
        }
    }

    private fun finishSuccess() {

        setResult(Activity.RESULT_OK)
        finish()
        if(continueFlow){
            gotoIntroSetup()
        }
    }



    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(AuthSetupViewModel::class.java)

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
                    finishSuccess()
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
        hideActionBarBack(this)
        passcode_view.passcodeListener = object : PasscodeView.PasscodeListener {
            override fun onInputChanged() {
                error_textview.setText("")
            }

            override fun onDone() {
                onConfirmClicked()
            }
        }
        full_password_button.setOnClickListener {
            gotoAuthSetupPassword()
        }
        passcode_view.requestFocus()
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun onConfirmClicked() {
        if (viewModel.checkPasswordRequirements(passcode_view.getPasscode())) {
            viewModel.startSetupPassword(passcode_view.getPasscode())
            gotoAuthSetupPasscodeRepeat()
        } else {
            passcode_view.clearPasscode()
            error_textview.setText(R.string.auth_error_passcode_not_valid)
        }
    }

    private fun gotoAuthSetupBiometrics() {
        val intent = Intent(this, AuthSetupBiometricsActivity::class.java)
        startActivityForResult(intent, REQUESTCODE_AUTH_SETUP_BIOMETRICS)
    }

    private fun gotoAuthSetupPasscodeRepeat() {
        val intent = Intent(this, AuthSetupRepeatActivity::class.java)
        startActivityForResult(intent, REQUESTCODE_AUTH_SETUP_PASSCODE_REPEAT)
    }

    private fun gotoAuthSetupPassword() {
        val intent = Intent(this, AuthSetupPasswordActivity::class.java)
        intent.putExtra(AuthSetupPasswordActivity.CONTINUE_INITIAL_SETUP, false);
        startActivityForResult(intent, REQUESTCODE_AUTH_SETUP_FULL_PASSWORD)
    }

    private fun gotoIntroSetup() {
        val intent = Intent(this, IntroSetupActivity::class.java)
        startActivity(intent)
    }

    private fun showPasswordError() {
        passcode_view.clearPasscode()
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(root_layout, R.string.auth_error_password_setup)
    }

    override fun loggedOut() {
    }

    //endregion


}
