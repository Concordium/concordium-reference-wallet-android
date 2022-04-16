package com.concordium.wallet.ui.base

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity.Companion.EXTRA_DELEGATION_DATA
import com.concordium.wallet.uicore.dialog.AuthenticationDialogFragment
import com.concordium.wallet.uicore.dialog.Dialogs
import com.concordium.wallet.uicore.popup.Popup
import java.io.Serializable
import javax.crypto.Cipher

abstract open class BaseActivity(private val layout: Int, private val titleId: Int = R.string.app_name) : AppCompatActivity() {

    private var titleView: TextView? = null
    private var subtitleView: TextView? = null
    protected lateinit var popup: Popup
    protected lateinit var dialogs: Dialogs

    companion object {
        const val REQUESTCODE_GENERIC_RETURN = 8232
        const val POP_UNTIL_ACTIVITY = "POP_UNTIL_ACTIVITY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!BuildConfig.DEBUG){
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        setContentView(layout)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        if (toolbar != null) {
            titleView = toolbar.findViewById<TextView>(R.id.toolbar_title)
            subtitleView = toolbar.findViewById<TextView>(R.id.toolbar_sub_title)
            setSupportActionBar(toolbar)
            setupActionBar(this, titleId)
        }

        popup = Popup()
        dialogs = Dialogs()

        App.appCore.session.isLoggedIn.observe(this, Observer<Boolean> { loggedin ->
            if(App.appCore.session.hasSetupPassword){
                if(loggedin){
                    loggedIn()
                }
                else{
                    loggedOut()
                }
            }
        })
    }

    /**
     * Upon returning, we check the result and pop if needed - see @onActivityResult
     */
    fun startActivityForResultAndHistoryCheck(intent: Intent) {
        startActivityForResult(intent, REQUESTCODE_GENERIC_RETURN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUESTCODE_GENERIC_RETURN) {
            if (resultCode == Activity.RESULT_OK) {
                data?.getStringExtra(POP_UNTIL_ACTIVITY)?.let { className ->
                    if (this.javaClass.asSubclass(this.javaClass).canonicalName != className) {
                        finishUntilClass(className, data.getStringExtra("THEN_START"), data.getStringExtra("WITH_KEY"), data.getSerializableExtra("WITH_DATA"))
                    } else {
                        data.getStringExtra("THEN_START")?.let { thenStart ->
                            val intent = Intent(this, Class.forName(thenStart))
                            if (data.getStringExtra("WITH_KEY") != null && data.getSerializableExtra("WITH_DATA") != null) {
                                intent.putExtra(data.getStringExtra("WITH_KEY"), data.getSerializableExtra("WITH_DATA"))
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
        }
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

    private fun setupActionBar(activity: AppCompatActivity, titleId: Int?) {
        val actionbar = activity.supportActionBar ?: return
        actionbar.setDisplayHomeAsUpEnabled(true)

        if (titleId != null) {
            actionbar.setTitle(titleId)
            titleView?.setText(titleId)
        }
    }

    fun setActionBarTitle(titleId: Int) {
        //supportActionBar?.setTitle(titleId)
        titleView?.setText(titleId)
    }

    fun setActionBarTitle(title: String) {
        //supportActionBar?.title = title
        titleView?.setText(title)
    }

    fun setActionBarTitle(title: String, subtitle: String?) {
        //supportActionBar?.title = title
        titleView?.setText(title)
        titleView?.setSingleLine()
        subtitleView?.setText(subtitle)
        subtitleView?.visibility = View.VISIBLE
    }

    fun hideActionBarBack(activity: AppCompatActivity) {
        val actionbar = activity.supportActionBar ?: return
        actionbar.setDisplayHomeAsUpEnabled(false)
    }

    // authentication region

    interface AuthenticationCallback {
        fun getCipherForBiometrics() : Cipher?
        fun onCorrectPassword(password: String)
        fun onCipher(cipher: Cipher)
        fun onCancelled()
    }

    protected fun showAuthentication(text: String?, shouldUseBiometrics: Boolean, usePasscode: Boolean, callback: AuthenticationCallback) {
        if (shouldUseBiometrics) {
            showBiometrics(text, usePasscode, callback)
        } else {
            showPasswordDialog(text, callback)
        }
    }

    fun authenticateText(useBiometrics: Boolean, usePasscode: Boolean): String {
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
        var biometricPrompt = createBiometricPrompt(text, callback)

        val promptInfo = createPromptInfo(text, usePasscode)
        val cipher = callback.getCipherForBiometrics()
        if (cipher != null) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun createBiometricPrompt(text: String?, callback: AuthenticationCallback): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPromptCallback() {
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

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        return biometricPrompt
    }

    private fun createPromptInfo(description: String?, usePasscode: Boolean): BiometricPrompt.PromptInfo {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_login_biometrics_dialog_title))
            .setDescription(description)
            .setConfirmationRequired(true)
            .setNegativeButtonText(getString(if (usePasscode) R.string.auth_login_biometrics_dialog_cancel_passcode else R.string.auth_login_biometrics_dialog_cancel_password))
            .build()
        return promptInfo
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        App.appCore.session.resetLogoutTimeout()
        return super.dispatchTouchEvent(event)
    }

    // end authentication region
}
