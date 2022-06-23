package com.concordium.wallet.ui.more.export

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.fragment_export_setup_password.*
import kotlinx.android.synthetic.main.fragment_export_setup_password.view.*


class ExportSetupPasswordFragment(val titleId: Int?=null) : BaseFragment(titleId) {

    private val viewModel: ExportViewModel by activityViewModels()

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
        initializeViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_export_setup_password, container, false)
        initializeViews(rootView)
        return rootView
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {

        viewModel.errorPasswordLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showPasswordError()
                }
            }
        })

        viewModel.errorNonIdenticalRepeatPasswordLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (!value) {
                    //Failure
                    password_edittext.setText("")
                    error_textview.setText(R.string.export_error_entries_different)
                }
            }
        })
    }

    private fun initializeViews(view: View) {
        view.confirm_button.setOnClickListener {
            onConfirmClicked()
        }
        view.confirm_button.isEnabled = false
        view.password_edittext.afterTextChanged {
            view.error_textview.setText("")
            view.confirm_button.isEnabled =
                viewModel.checkPasswordRequirements(view.password_edittext.text.toString())
        }
        view.password_edittext.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (viewModel.checkPasswordRequirements(view.password_edittext.text.toString())) {
                        onConfirmClicked()
                    }
                    true
                }
                else -> false
            }
        }

        Handler().post(object: Runnable {
            override fun run() {
                context?.let { KeyboardUtil.showKeyboard(it, password_edittext) }
            }
        })
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun onConfirmClicked() {
        if (viewModel.checkPasswordRequirements(password_edittext.text.toString())) {
            viewModel.setStartExportPassword(password_edittext.text.toString())
        } else {
            password_edittext.setText("")
            error_textview.setText(R.string.export_error_password_not_valid)
        }
    }

    private fun showPasswordError() {
        password_edittext.setText("")
        KeyboardUtil.hideKeyboard(activity!!.parent)
        popup.showSnackbar(root_layout, R.string.export_error_password_setup)
    }

    //endregion


}
