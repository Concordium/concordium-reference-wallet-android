package com.concordium.wallet.ui.more.alterpassword

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityAlterpasswordBinding
import com.concordium.wallet.ui.auth.setup.AuthSetupActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.KeyboardUtil
import javax.crypto.Cipher

class AlterPasswordActivity : BaseActivity() {
    private lateinit var binding: ActivityAlterpasswordBinding
    private val viewModel: AlterPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlterpasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.alterpassword_title)

        viewModel.initialize()

        showWaiting(false)

        binding.confirmButton.setOnClickListener {
            viewModel.checkAndStartPasscodeChange()
        }

        viewModel.checkAccountsIdentitiesDoneLiveData.observe(this) { success ->
            if (success) {
                showAuthentication(null, object : AuthenticationCallback {
                    override fun getCipherForBiometrics(): Cipher? {
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
            } else {
                Toast.makeText(this,
                    getString(R.string.alterpassword_non_finalised_items),
                    Toast.LENGTH_LONG).show()
            }
        }

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })

        viewModel.doneInitialAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                val intent = Intent(baseContext, AuthSetupActivity::class.java)
                intent.putExtra(AuthSetupActivity.CONTINUE_INITIAL_SETUP, false)
                getResultAuthReset.launch(intent)
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

    private val getResultAuthReset =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                App.appCore.session.tempPassword?.let { tempPassword ->
                    viewModel.finishPasswordChange(tempPassword)
                }
            }
        }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }

    private fun showError(stringRes: Int) {
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(binding.rootLayout, stringRes)
    }
}
