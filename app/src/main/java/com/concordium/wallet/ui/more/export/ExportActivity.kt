package com.concordium.wallet.ui.more.export

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.concordium.wallet.DataFileProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseActivity.AuthenticationCallback
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_export.*
import kotlinx.android.synthetic.main.progress.*
import java.io.File
import javax.crypto.Cipher


class ExportActivity : BaseActivity(
    R.layout.activity_export,
    R.string.export_title
) {

    private var isShareFlowActive: Boolean = false
    private val BACKSTACK_NAME_PASSWORD = "BACKSTACK_NAME_PASSWORD"
    private val BACKSTACK_NAME_REPEAT_PASSWORD = "BACKSTACK_NAME_REPEAT_PASSWORD"


    companion object {

    }

    private val viewModel: ExportViewModel by viewModels()



    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize()
        initViews()
        if(null == savedInstanceState) {
            replaceFragment(ExportFragment(R.string.export_title), "",false);
        }
    }

    override fun onPause() {
        super.onPause()  //called when share popup is shown
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
                    shareExportFile()
                }
            }
        })

        viewModel.finishRepeatPasswordScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                supportFragmentManager.popBackStack(BACKSTACK_NAME_REPEAT_PASSWORD, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                if (value) {
                    //Success
                    supportFragmentManager.popBackStack(BACKSTACK_NAME_PASSWORD, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    viewModel.finalizeEncryptionOfFile();
                }
                else{
                    //Failure
                }
            }
        })

    }




    private fun initViews() {
        progress_layout.visibility = View.GONE
    }

    //endregion

    //region Control
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        progress_layout.visibility = if(waiting) View.VISIBLE else View.GONE
    }

    private fun showError(stringRes: Int) {
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(root_layout, stringRes)
    }


    private fun showRequestExportPassword() {
        replaceFragment(ExportSetupPasswordFragment(R.string.export_setup_password_title), BACKSTACK_NAME_PASSWORD, true)
    }

    private fun showRepeatExportPassword() {
        replaceFragment(ExportSetupPasswordRepeatFragment(R.string.export_setup_password_repeat_title), BACKSTACK_NAME_REPEAT_PASSWORD,true)
    }


    private fun shareExportFile() {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "message/rfc822"
        val uri = Uri.parse("content://" + DataFileProvider.AUTHORITY.toString() + File.separator.toString() + ExportViewModel.FILE_NAME);
        share.putExtra(Intent.EXTRA_STREAM, uri)
        val resInfoList = this.packageManager.queryIntentActivities(share, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(share, null));
        isShareFlowActive = true
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
