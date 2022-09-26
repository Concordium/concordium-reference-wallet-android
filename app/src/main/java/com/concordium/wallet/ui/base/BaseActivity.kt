package com.concordium.wallet.ui.base

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.storage.StorageManager
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.uicore.dialog.AuthenticationDialogFragment
import com.concordium.wallet.uicore.dialog.Dialogs
import com.concordium.wallet.uicore.popup.Popup
import java.io.Serializable
import javax.crypto.Cipher

abstract class BaseActivity : AppCompatActivity() {

    private var titleView: TextView? = null
    protected lateinit var popup: Popup
    protected lateinit var dialogs: Dialogs
    public var isActive = false

    companion object {
        const val POP_UNTIL_ACTIVITY = "POP_UNTIL_ACTIVITY"
        const val RESULT_FOLDER_PICKER = 101
        const val RESULT_SHARE_FILE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BuildConfig.DEBUG) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        popup = Popup()
        dialogs = Dialogs()

        App.appCore.session.isLoggedIn.observe(this) { loggedIn ->
            if (App.appCore.session.hasSetupPassword) {
                if (loggedIn) {
                    loggedIn()
                } else {
                    loggedOut()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActive = true
    }

    override fun onPause() {
        super.onPause()
        isActive = false
    }

    protected fun shareFile(fileName: Uri) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "message/rfc822"
        share.putExtra(Intent.EXTRA_STREAM, fileName)
        val resInfoList = this.packageManager.queryIntentActivities(share, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            grantUriPermission(packageName, fileName, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(Intent.createChooser(share, null), RESULT_SHARE_FILE)
    }

    // Upon returning, we check the result and pop if needed
    private val getResultGeneric =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(POP_UNTIL_ACTIVITY)?.let { className ->
                    if (this.javaClass.asSubclass(this.javaClass).canonicalName != className) {
                        finishUntilClass(className, it.data?.getStringExtra("THEN_START"), it.data?.getStringExtra("WITH_KEY"), it.data?.getSerializableExtra("WITH_DATA"))
                    } else {
                        it.data?.getStringExtra("THEN_START")?.let { thenStart ->
                            val intent = Intent(this, Class.forName(thenStart))
                            if (it.data?.getStringExtra("WITH_KEY") != null && it.data?.getSerializableExtra("WITH_DATA") != null) {
                                intent.putExtra(it.data?.getStringExtra("WITH_KEY"), it.data?.getSerializableExtra("WITH_DATA"))
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
        }

    fun startActivityForResultAndHistoryCheck(intent: Intent) {
        getResultGeneric.launch(intent)
    }

    protected fun openFolderPicker() {
        val intent: Intent?
        if (SDK_INT >= Build.VERSION_CODES.Q) {
            val storageManager = getSystemService(STORAGE_SERVICE) as StorageManager
            intent = storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
            val startDir = "Documents"
            var uriRoot = intent.getParcelableExtra<Uri>("android.provider.extra.INITIAL_URI")
            var scheme = uriRoot.toString()
            scheme = scheme.replace("/root/", "/document/")
            scheme += "%3A$startDir"
            uriRoot = Uri.parse(scheme)
            intent.putExtra("android.provider.extra.INITIAL_URI", uriRoot)
        } else {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        }
        intent.apply { flags = Intent.FLAG_GRANT_READ_URI_PERMISSION }
        startActivityForResult(intent, RESULT_FOLDER_PICKER)
    }

    fun finishUntilClass(canonicalClassName: String?, thenStart: String? = null, withKey: String? = null, withData: Serializable? = null) {
        canonicalClassName?.let {
            val intent = Intent()
            intent.putExtra(POP_UNTIL_ACTIVITY, it)
            thenStart?.let {
                intent.putExtra("THEN_START", thenStart)
                if (withKey != null && withData != null) {
                    intent.putExtra("WITH_KEY", withKey)
                    intent.putExtra("WITH_DATA", withData)
                }
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    open fun loggedOut() {
        val intent = Intent(this, AuthLoginActivity::class.java)
        startActivity(intent)
    }

    open fun loggedIn() {
        //do nothing
    }

    protected fun setupActionBar(toolbar: Toolbar, titleView: TextView, titleId: Int?) {
        this.titleView = titleView

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (titleId != null) {
            supportActionBar?.setTitle(titleId)
            titleView.setText(titleId)
        }
    }

    fun setActionBarTitle(titleId: Int) {
        titleView?.setText(titleId)
    }

    fun setActionBarTitle(title: String) {
        titleView?.text = title
    }

    fun setActionBarTitle(subtitleView: TextView, title: String, subtitle: String?) {
        titleView?.text = title
        titleView?.setSingleLine()
        subtitleView.text = subtitle
        subtitleView.visibility = View.VISIBLE
    }

    fun showActionBarBack() {
        val actionbar = supportActionBar ?: return
        actionbar.setDisplayHomeAsUpEnabled(true)
    }

    fun hideActionBarBack() {
        val actionbar = supportActionBar ?: return
        actionbar.setDisplayHomeAsUpEnabled(false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    // authentication region

    interface AuthenticationCallback {
        fun getCipherForBiometrics() : Cipher?
        fun onCorrectPassword(password: String)
        fun onCipher(cipher: Cipher)
        fun onCancelled()
    }

    protected fun showAuthentication(text: String?, callback: AuthenticationCallback) {
        val useBiometrics = App.appCore.getCurrentAuthenticationManager().useBiometrics()
        val usePasscode = App.appCore.getCurrentAuthenticationManager().usePasscode()
        if (useBiometrics) {
            showBiometrics(text, usePasscode, callback)
        } else {
            showPasswordDialog(text, callback)
        }
    }

    fun authenticateText(): String {
        val useBiometrics = App.appCore.getCurrentAuthenticationManager().useBiometrics()
        val usePasscode = App.appCore.getCurrentAuthenticationManager().usePasscode()
        return when {
            useBiometrics -> getString(R.string.auth_login_biometrics_dialog_subtitle)
            usePasscode -> getString(R.string.auth_login_biometrics_dialog_cancel_passcode)
            else -> getString(R.string.auth_login_biometrics_dialog_cancel_password)
        }
    }

    private fun showPasswordDialog(text: String?, callback: AuthenticationCallback) {
        val dialogFragment = AuthenticationDialogFragment()
        dialogFragment.isCancelable = false
        if(text != null){
            val bundle = Bundle()
            bundle.putString(AuthenticationDialogFragment.EXTRA_ALTERNATIVE_TEXT, text)
            dialogFragment.arguments = bundle
        }
        dialogFragment.setCallback(object : AuthenticationDialogFragment.Callback {
            override fun onCorrectPassword(password: String) {
                callback.onCorrectPassword(password)
            }

            override fun onCancelled() {
                callback.onCancelled()
            }
        })
        dialogFragment.show(supportFragmentManager, AuthenticationDialogFragment.AUTH_DIALOG_TAG)
    }

    private fun showBiometrics(
        text: String?,
        usePasscode: Boolean,
        callback: AuthenticationCallback
    ) {
        val biometricPrompt = createBiometricPrompt(text, callback)

        val promptInfo = createPromptInfo(text, usePasscode)
        val cipher = callback.getCipherForBiometrics()
        if (cipher != null) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun createBiometricPrompt(
        text: String?,
        callback: AuthenticationCallback
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        @Suppress("NAME_SHADOWING") val callback = object : BiometricPromptCallback() {
            override fun onNegativeButtonClicked() {
                showPasswordDialog(text, callback)
            }

            override fun onAuthenticationSucceeded(cipher: Cipher) {
                callback.onCipher(cipher)
            }

            override fun onUserCancelled() {
                super.onUserCancelled()
                callback.onCancelled()
            }
        }

        return BiometricPrompt(this, executor, callback)
    }

    private fun createPromptInfo(
        description: String?,
        usePasscode: Boolean
    ): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_login_biometrics_dialog_title))
            .setDescription(description)
            .setConfirmationRequired(true)
            .setNegativeButtonText(getString(if (usePasscode) R.string.auth_login_biometrics_dialog_cancel_passcode else R.string.auth_login_biometrics_dialog_cancel_password))
            .build()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        App.appCore.session.resetLogoutTimeout()
        return super.dispatchTouchEvent(event)
    }

    // end authentication region
}
