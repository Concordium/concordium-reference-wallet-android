package com.concordium.wallet.uicore.dialog

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
import com.concordium.wallet.databinding.DialogAuthenticationContainerBinding
import com.concordium.wallet.uicore.afterTextChanged

class AuthenticationDialogFragment : DialogFragment(), TextView.OnEditorActionListener {
    private var _binding: DialogAuthenticationContainerBinding? = null
    private val binding get() = _binding!!

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogAuthenticationContainerBinding.inflate(inflater, container, false)
        dialog?.setTitle(getString(R.string.auth_dialog_password_title))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val alternativeString = arguments?.getString(EXTRA_ALTERNATIVE_TEXT)
        if(alternativeString != null) {
            binding.includeDialogAuthenticationContent.passwordDescription.text = alternativeString
        } else {
            binding.includeDialogAuthenticationContent.passwordDescription.setText(if (App.appCore.getCurrentAuthenticationManager().usePasscode()) R.string.auth_dialog_passcode_description else R.string.auth_dialog_password_description)
        }

        binding.includeDialogAuthenticationContent.passwordEdittext.setHint(if (App.appCore.getCurrentAuthenticationManager().usePasscode()) R.string.auth_dialog_passcode else R.string.auth_dialog_password)
        if (App.appCore.getCurrentAuthenticationManager().usePasscode()) {
            binding.includeDialogAuthenticationContent.passwordEdittext.inputType =
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        binding.includeDialogAuthenticationContent.passwordEdittext.setOnEditorActionListener(this)
        binding.includeDialogAuthenticationContent.passwordEdittext.afterTextChanged {
            binding.includeDialogAuthenticationContent.passwordError.text = ""
        }
        binding.secondDialogButton.setOnClickListener {
            verifyPassword()
        }
        binding.cancelButton.setOnClickListener {
            callback?.onCancelled()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    private fun verifyPassword() {
        val passwordIsValid = App.appCore.getCurrentAuthenticationManager().checkPassword(binding.includeDialogAuthenticationContent.passwordEdittext.text.toString())
        if (passwordIsValid) {
            callback?.onCorrectPassword(binding.includeDialogAuthenticationContent.passwordEdittext.text.toString())
            binding.includeDialogAuthenticationContent.passwordEdittext.setText("")
            dismiss()
        } else {
            binding.includeDialogAuthenticationContent.passwordEdittext.setText("")
            binding.includeDialogAuthenticationContent.passwordError.setText(if (App.appCore.getCurrentAuthenticationManager().usePasscode()) R.string.auth_dialog_passcode_error else R.string.auth_dialog_password_error)
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
