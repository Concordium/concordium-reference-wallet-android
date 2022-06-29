package com.concordium.wallet.ui.more.export

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentExportSetupPasswordBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.util.KeyboardUtil

class ExportSetupPasswordRepeatFragment(val titleId: Int?= null) : BaseFragment(titleId) {
    private var _binding: FragmentExportSetupPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExportViewModel by activityViewModels()

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentExportSetupPasswordBinding.inflate(inflater, container, false)
        initializeViews(binding.root)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViews(view: View) {
        binding.instructionTextview.setText(R.string.export_setup_password_repeat_info)
        binding.confirmButton.setOnClickListener {
            onConfirmClicked()
        }
        binding.passwordEdittext.setOnEditorActionListener { _, actionId, _ ->
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
                context?.let { KeyboardUtil.showKeyboard(it, binding.passwordEdittext) }
            }
        })
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun onConfirmClicked() {
        viewModel.checkExportPassword(binding.passwordEdittext.text.toString())
    }

    //endregion
}
