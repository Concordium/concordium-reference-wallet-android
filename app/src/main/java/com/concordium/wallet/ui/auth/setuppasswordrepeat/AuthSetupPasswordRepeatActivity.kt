package com.concordium.wallet.ui.auth.setuppasswordrepeat

import android.app.Activity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_auth_setup_password.*

class AuthSetupPasswordRepeatActivity :
    BaseActivity(R.layout.activity_auth_setup_password, R.string.auth_setup_password_repeat_title) {

    private lateinit var viewModel: AuthSetupPasswordRepeatViewModel


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
        ).get(AuthSetupPasswordRepeatViewModel::class.java)

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
        instruction_textview.setText(R.string.auth_setup_password_repeat_info)
        confirm_button.setOnClickListener {
            onConfirmClicked()
        }
        password_edittext.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    onConfirmClicked()
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
        viewModel.checkPassword(password_edittext.text.toString())
    }

    //endregion


}
