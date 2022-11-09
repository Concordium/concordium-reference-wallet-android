package com.concordium.wallet.ui.more.import

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.dialog.AuthenticationDialogFragment
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_import.*
import kotlinx.android.synthetic.main.progress.*
import javax.crypto.Cipher

class ImportActivity : BaseActivity(
    R.layout.activity_import,
    R.string.import_title
) {

    companion object {
        const val EXTRA_FILE_URI = "EXTRA_FILE_URI"
        const val REQUESTCODE_SELECT_FILE = 2000
    }

    private val viewModel: ImportViewModel by viewModels()
    private lateinit var biometricPrompt: BiometricPrompt
    private var allowBack = true


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize()
        initViews()
        if (null != savedInstanceState) {
            // Restoring state (screen orientation changed) - Do not add fragment or show dialog
            // again, because we will end up with multiples of the same fragment.
            return
        }

        val fileUri = intent?.getParcelableExtra<Uri>(EXTRA_FILE_URI)
        if (fileUri != null) {
            handleImportFile(fileUri)
        } else {
            showFilePicker()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUESTCODE_SELECT_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.data?.also { uri ->
                    handleImportFile(uri)
                }
            } else {
                finish()
            }
        }
    }

    override fun onBackPressed() {
        if (allowBack) {
            super.onBackPressed()
        }
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {

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
        viewModel.showImportPasswordLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showImportPassword()
                }
            }
        })
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication()
                }
            }
        })
        viewModel.showImportConfirmedLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showImportConfirmed()
                }
            }
        })
        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    App.appCore.session.setAccountsBackedUp(true)
                    finish()
                    val intent = Intent(this@ImportActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
            }
        })

        viewModel.errorAndFinishLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showFailedImport(value)
            }
        })
    }

    private fun initViews() {
        progress_layout.visibility = View.VISIBLE
    }

    //endregion

    //region Control
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    private fun showError(stringRes: Int) {
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(root_layout, stringRes)
    }

    private fun showImportPassword() {
        replaceFragment(
            ImportPasswordFragment(R.string.import_password_title),
            "",
            false
        )
    }

    private fun showAuthentication() {
        if (viewModel.shouldUseBiometrics()) {
            showBiometrics()
        } else {
            showPasswordDialog()
        }
    }

    private fun showImportConfirmed() {
        replaceFragment(
            ImportConfirmedFragment(R.string.import_confirmed_title),
            "",
            true
        )
        allowBack = false
    }

    private fun showFailedImport(message: Int) {
        replaceFragment(
            ImportFailedFragment(message, R.string.import_confirmed_title),
            "",
            true
        )
        allowBack = true
    }

    private fun showPasswordDialog() {
        val dialogFragment = AuthenticationDialogFragment()
        dialogFragment.setCallback(object : AuthenticationDialogFragment.Callback {
            override fun onCorrectPassword(password: String) {
                viewModel.checkLogin(password)
            }

            override fun onCancelled() {
            }
        })
        dialogFragment.show(supportFragmentManager, AuthenticationDialogFragment.AUTH_DIALOG_TAG)
    }

    private fun replaceFragment(fragment: Fragment, name: String, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(name)
        }
        transaction.commit()
    }

    private fun showFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, REQUESTCODE_SELECT_FILE)
    }

    private fun handleImportFile(uri: Uri) {
        viewModel.handleImportFile(ImportFile(uri))
    }

    //endregion

    //region Biometrics
    //************************************************************

    private fun showBiometrics() {
        biometricPrompt = createBiometricPrompt()

        val promptInfo = createPromptInfo()

        val cipher = viewModel.getCipherForBiometrics()
        if (cipher != null) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPromptCallback() {
            override fun onNegativeButtonClicked() {
                showPasswordDialog()
            }

            override fun onAuthenticationSucceeded(cipher: Cipher) {
                viewModel.checkLogin(cipher)
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        return biometricPrompt
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_login_biometrics_dialog_title))
            .setConfirmationRequired(true)
            .setNegativeButtonText(getString(if (viewModel.usePasscode()) R.string.auth_login_biometrics_dialog_cancel_passcode else R.string.auth_login_biometrics_dialog_cancel_password))
            .build()
        return promptInfo
    }

    //endregion
}
