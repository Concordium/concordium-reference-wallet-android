package com.concordium.wallet.ui.more.export

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.fragment_export_setup_password.*
import kotlinx.android.synthetic.main.fragment_export_setup_password.view.*


class ExportSetupPasswordRepeatFragment(val titleId: Int?= null) : BaseFragment(titleId) {

    private val viewModel: ExportViewModel by activityViewModels()

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    }

    private fun initializeViews(view: View) {
        view.instruction_textview.setText(R.string.export_setup_password_repeat_info)
        view.confirm_button.setOnClickListener {
            onConfirmClicked()
        }
        view.password_edittext.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    onConfirmClicked()
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
        viewModel.checkExportPassword(password_edittext.text.toString())
    }

    //endregion


}
