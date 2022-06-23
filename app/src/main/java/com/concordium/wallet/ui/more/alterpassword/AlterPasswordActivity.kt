package com.concordium.wallet.ui.more.alterpassword

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.ui.auth.setup.AuthSetupActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_alterpassword.confirm_button
import kotlinx.android.synthetic.main.activity_auth_login.*
import kotlinx.android.synthetic.main.activity_auth_setup.passcode_view
import kotlinx.android.synthetic.main.activity_auth_setup.root_layout
import kotlinx.android.synthetic.main.progress.*
import javax.crypto.Cipher

class AlterPasswordActivity : BaseActivity(
    R.layout.activity_alterpassword,
    R.string.alterpassword_title
) {

    //region Lifecycle
    //************************************************************

    private val REQUESTCODE_AUTH_RESET = 3001

    private val viewModel: AlterPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()

        showWaiting(false)

        confirm_button.setOnClickListener {
            viewModel.checkAndStartPasscodeChange()
        }


        viewModel.checkAccountsIdentitiesDoneLiveData.observe(this, Observer<Boolean> { success ->
            if(success){
                showAuthentication(null, viewModel.shouldUseBiometrics(), viewModel.usePasscode, object : AuthenticationCallback{
                    override fun getCipherForBiometrics() : Cipher?{
                        return viewModel.getCipherForBiometrics()
                    }
                    override fun onCorrectPassword(password: String) {
                        viewModel.checkLogin(password)
                    }
                    override fun onCipher(cipher: Cipher) {
                        viewModel.checkLogin(cipher)
                    }
                    override fun onCancelled() {
                    }
                })
            }
            else{
                Toast.makeText(this,getString(R.string.alterpassword_non_finalised_items), Toast.LENGTH_LONG).show()
            }
        })

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })

        viewModel.doneInitialAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                val intent = Intent(baseContext, AuthSetupActivity::class.java)
                intent.putExtra(AuthSetupActivity.CONTINUE_INITIAL_SETUP, false);
                startActivityForResult(intent, REQUESTCODE_AUTH_RESET)
            }
        })

        viewModel.errorInitialAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                Toast.makeText(baseContext, getString(R.string.change_password_initial_error), Toast.LENGTH_LONG).show()
            }
        })


        viewModel.doneFinalChangePasswordLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                Toast.makeText(baseContext, getString(R.string.change_password_successfully_changed), Toast.LENGTH_LONG).show()
                finish()
            }
        })

        viewModel.errorFinalChangePasswordLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                Toast.makeText(baseContext, getString(R.string.change_password_final_error), Toast.LENGTH_LONG).show()
            }
        })


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUESTCODE_AUTH_RESET) {
            if (resultCode == Activity.RESULT_OK) {
                App.appCore.session.tempPassword?.let {
                    viewModel.finishPasswordChange(it)
                }
            }
        }
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    private fun showError(stringRes: Int) {
        password_edittext.setText("")
        passcode_view.clearPasscode()
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(root_layout, stringRes)
    }

    private fun initializeViewModel() {

    }

    //endregion

}
