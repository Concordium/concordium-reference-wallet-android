package com.concordium.wallet.ui.more.export

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityExportBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.dialog.AuthenticationDialogFragment
import com.concordium.wallet.util.KeyboardUtil
import javax.crypto.Cipher

class ExportActivity : BaseActivity() {
    private var isShareFlowActive: Boolean = false
    private val BACKSTACK_NAME_PASSWORD = "BACKSTACK_NAME_PASSWORD"
    private val BACKSTACK_NAME_REPEAT_PASSWORD = "BACKSTACK_NAME_REPEAT_PASSWORD"

    private lateinit var binding: ActivityExportBinding
    private val viewModel: ExportViewModel by viewModels()

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeViewModel()
        viewModel.initialize()
        initViews()
        if(null == savedInstanceState) {
            replaceFragment(ExportFragment(R.string.export_title), "",false)
        }
    }

    override fun onResume() {
        super.onResume()
        isShareFlowActive = false
    }

    override fun onStop() {
        super.onStop()  //called when app shared with is started
        if(isShareFlowActive){
            finish()
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
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(null, viewModel.shouldUseBiometrics(), viewModel.usePasscode(), object : AuthenticationCallback{
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
            }
        })
        viewModel.showRepeatPasswordScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showRepeatExportPassword()
                }
            }
        })
        viewModel.showRequestPasswordLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showRequestExportPassword()
                }
            }
        })
        viewModel.shareExportFileLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    shareFile(viewModel.getEncryptedFileWithPath())
                    App.appCore.session.setAccountsBackedUp(true)
                    isShareFlowActive = true
                }
            }
        })

        viewModel.finishRepeatPasswordScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                supportFragmentManager.popBackStack(BACKSTACK_NAME_REPEAT_PASSWORD, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                if (value) {
                    //Success
                    supportFragmentManager.popBackStack(BACKSTACK_NAME_PASSWORD, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    chooseLocalFolderOrShareWithApp()
                }
                else{
                    //Failure
                }
            }
        })
    }

    private fun initViews() {
        binding.includeProgress.progressLayout.visibility = View.GONE
    }

    //endregion

    //region Control
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressLayout.visibility = if(waiting) View.VISIBLE else View.GONE
    }

    private fun showError(stringRes: Int) {
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(binding.rootLayout, stringRes)
    }

    private fun showRequestExportPassword() {
        replaceFragment(ExportSetupPasswordFragment(R.string.export_setup_password_title), BACKSTACK_NAME_PASSWORD, true)
    }

    private fun showRepeatExportPassword() {
        replaceFragment(ExportSetupPasswordRepeatFragment(R.string.export_setup_password_repeat_title), BACKSTACK_NAME_REPEAT_PASSWORD,true)
    }

    private fun chooseLocalFolderOrShareWithApp() {
        val dialogFragment = ExportChooseMethodFragment()
        dialogFragment.isCancelable = false
        dialogFragment.setCallback(object : ExportChooseMethodFragment.Callback {
            override fun onAnotherApp() {
                viewModel.finalizeEncryptionOfFile()
            }
            override fun onLocalStorage() {
                openFolderPicker()
            }
        })
        dialogFragment.show(supportFragmentManager, AuthenticationDialogFragment.AUTH_DIALOG_TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == RESULT_FOLDER_PICKER) {
            data?.data?.let { uri ->
                viewModel.saveFileToLocalFolder(uri)
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, name: String, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(name)
        }
        transaction.commit()
    }
    //endregion
}
