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
import com.concordium.wallet.databinding.FragmentExportSetupPasswordBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil

class ExportSetupPasswordFragment(val titleId: Int?=null) : BaseFragment(titleId) {
    private var _binding: FragmentExportSetupPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExportViewModel by activityViewModels()

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
        initializeViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentExportSetupPasswordBinding.inflate(inflater, container, false)
        initializeViews(binding.root)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                    binding.passwordEdittext.setText("")
                    binding.errorTextview.setText(R.string.export_error_entries_different)
                }
            }
        })
    }

    private fun initializeViews(view: View) {
        binding.confirmButton.setOnClickListener {
            onConfirmClicked()
        }
        binding.confirmButton.isEnabled = false
        binding.passwordEdittext.afterTextChanged {
            binding.errorTextview.setText("")
            binding.confirmButton.isEnabled =
                viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())
        }
        binding.passwordEdittext.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())) {
                        onConfirmClicked()
                    }
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
        if (viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())) {
            viewModel.setStartExportPassword(binding.passwordEdittext.text.toString())
        } else {
            binding.passwordEdittext.setText("")
            binding.errorTextview.setText(R.string.export_error_password_not_valid)
        }
    }

    private fun showPasswordError() {
        binding.passwordEdittext.setText("")
        KeyboardUtil.hideKeyboard(requireActivity().parent)
        popup.showSnackbar(binding.rootLayout, R.string.export_error_password_setup)
    }

    //endregion
}
