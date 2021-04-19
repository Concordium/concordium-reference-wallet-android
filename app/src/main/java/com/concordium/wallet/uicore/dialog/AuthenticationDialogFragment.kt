package com.concordium.wallet.uicore.dialog

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.uicore.afterTextChanged
import kotlinx.android.synthetic.main.dialog_authentication_container.view.*
import kotlinx.android.synthetic.main.dialog_authentication_content.*
import kotlinx.android.synthetic.main.dialog_authentication_content.view.*

class AuthenticationDialogFragment : DialogFragment(),
    TextView.OnEditorActionListener {

    companion object {
        const val AUTH_DIALOG_TAG = "AUTH_DIALOG_TAG"
        const val EXTRA_ALTERNATIVE_TEXT = "EXTRA_ALTERNATIVE_TEXT"
    }

    private var callback: Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        retainInstance = true
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.setTitle(getString(R.string.auth_dialog_password_title))
        return inflater.inflate(R.layout.dialog_authentication_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val alternativeString = arguments?.getString(EXTRA_ALTERNATIVE_TEXT)
        if(alternativeString != null) {
            view.password_description.setText(alternativeString)
        } else {
            view.password_description.setText(if (App.appCore.getCurrentAuthenticationManager().usePasscode()) R.string.auth_dialog_passcode_description else R.string.auth_dialog_password_description)
        }

        view.password_edittext.setHint(if (App.appCore.getCurrentAuthenticationManager().usePasscode()) R.string.auth_dialog_passcode else R.string.auth_dialog_password)
        if (App.appCore.getCurrentAuthenticationManager().usePasscode()) {
            view.password_edittext.inputType =
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        view.password_edittext.setOnEditorActionListener(this)
        view.password_edittext.afterTextChanged {
            password_error.setText("")
        }
        view.second_dialog_button.setOnClickListener {
            verifyPassword()
        }
        view.cancel_button.setOnClickListener {
            callback?.onCancelled()
            dismiss()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    private fun verifyPassword() {
        val passwordIsValid = App.appCore.getCurrentAuthenticationManager().checkPassword(password_edittext.text.toString())
        if (passwordIsValid) {
            callback?.onCorrectPassword(password_edittext.text.toString())
            password_edittext.setText("")
            dismiss()
        } else {
            password_edittext.setText("")
            password_error.setText(if (App.appCore.getCurrentAuthenticationManager().usePasscode()) R.string.auth_dialog_passcode_error else R.string.auth_dialog_password_error)
        }
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        return if (actionId == EditorInfo.IME_ACTION_GO) {
            verifyPassword()
            true
        } else false
    }

    interface Callback {
        fun onCorrectPassword(password: String)
        fun onCancelled()
    }
}
