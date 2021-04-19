package com.concordium.wallet.ui.auth.setuprepeat

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.view.PasscodeView
import kotlinx.android.synthetic.main.activity_auth_setup.*

class AuthSetupRepeatActivity :
    BaseActivity(R.layout.activity_auth_setup, R.string.auth_setup_repeat_title) {

    private lateinit var viewModel: AuthSetupRepeatViewModel


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        ).get(AuthSetupRepeatViewModel::class.java)

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
        instruction_textview.setText(R.string.auth_setup_repeat_info)
        passcode_view.passcodeListener = object : PasscodeView.PasscodeListener {
            override fun onInputChanged() {
            }

            override fun onDone() {
                onConfirmClicked()
            }
        }
        full_password_button.visibility = View.GONE
        passcode_view.requestFocus()
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun onConfirmClicked() {
        viewModel.checkPassword(passcode_view.getPasscode())
    }

    //endregion


}
