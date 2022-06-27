package com.concordium.wallet.ui.intro.introstart

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.model.AppSettings
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_intro_terms.*

class IntroTermsActivity : BaseActivity(R.layout.activity_intro_terms, R.string.terms_title) {

    private lateinit var viewModel: IntroTermsViewModel
    private var forceUpdateDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        initializeViewModel()
        viewModel.checkForExistingWallet()
    }

    override fun onBackPressed() {
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(IntroTermsViewModel::class.java)

        viewModel.hasExistingWalletLiveData.observe(this, Observer { hasExistingWallet ->
            if (!hasExistingWallet && !App.appCore.appSettingsForceUpdateChecked)
                viewModel.loadAppSettings()
            else
                confirm_button.isEnabled = true
        })

        viewModel.appSettingsLiveData.observe(this, Observer { appSettings ->
            checkAppSettings(appSettings)
            confirm_button.isEnabled = true
        })
    }

    private fun initViews() {
        confirm_button.isEnabled = false
        confirm_button.setOnClickListener {
            gotoStart()
        }

        val html = getString(R.string.terms_text)
        info_webview.isVerticalScrollBarEnabled = false
        info_webview.loadData(html, "text/html", "UTF-8")
    }

    private fun checkAppSettings(appSettings: AppSettings?) {
        appSettings?.let {
            when (it.status) {
                AppSettings.APP_VERSION_STATUS_WARNING -> it.url?.let { url -> showAppUpdateWarning(url) }
                AppSettings.APP_VERSION_STATUS_NEEDS_UPDATE -> it.url?.let { url -> showAppUpdateNeedsUpdate(url) }
                else -> {}
            }
        }
    }

    private fun showAppUpdateWarning(url: String) {
        if (forceUpdateDialog != null)
            return

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.force_update_intro_warning_title)
        builder.setMessage(getString(R.string.force_update_intro_warning_message))
        builder.setPositiveButton(getString(R.string.force_update_intro_warning_update_now)) { _, _ ->
            gotoAppStore(url)
        }
        builder.setNeutralButton(getString(R.string.force_update_intro_warning_remind_me)) { dialog, _ ->
            App.appCore.appSettingsForceUpdateChecked = true
            dialog.dismiss()
        }
        builder.setCancelable(false)
        forceUpdateDialog = builder.create()
        forceUpdateDialog?.show()
    }

    private fun showAppUpdateNeedsUpdate(url: String) {
        if (forceUpdateDialog != null)
            return

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.force_update_intro_needed_title)
        builder.setMessage(getString(R.string.force_update_intro_needed_message))
        builder.setPositiveButton(getString(R.string.force_update_intro_needed_update_now)) { _, _ ->
            gotoAppStore(url)
        }
        builder.setCancelable(false)
        forceUpdateDialog = builder.create()
        forceUpdateDialog?.show()
    }

    private fun gotoAppStore(url: String) {
        if (url.isNotBlank())
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        Process.killProcess(Process.myPid())
    }

    private fun gotoStart() {
        App.appCore.session.setTermsHashed(App.appContext.getString(R.string.terms_text).hashCode())
        finish()
        val intent = if(App.appCore.session.hasSetupPassword) Intent(this, AuthLoginActivity::class.java) else Intent(this, IntroStartActivity::class.java)
        startActivity(intent)
    }

    override fun loggedOut() {
        // do nothing as we are one of the root activities
    }
}
