package com.concordium.wallet.ui.more.import

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentImportPasswordBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil

class ImportPasswordFragment(titleId: Int? = null) : BaseFragment(titleId) {
    private var _binding: FragmentImportPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ImportViewModel by activityViewModels()

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
        initializeViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImportPasswordBinding.inflate(inflater, container, false)
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
        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
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
                    onConfirmClicked()
                    true
                }
                else -> false
            }
        }
        Handler().post(object : Runnable {
            override fun run() {
                context?.let { KeyboardUtil.showKeyboard(it, binding.passwordEdittext) }
            }
        })
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.passwordEdittext.isEnabled = false
            binding.confirmButton.isEnabled = false
        } else {
            binding.passwordEdittext.isEnabled = true
            binding.confirmButton.isEnabled = true
        }
    }

    private fun onConfirmClicked() {
        if (viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())) {
            viewModel.startImport(binding.passwordEdittext.text.toString())
        } else {
            binding.passwordEdittext.setText("")
            binding.errorTextview.setText(R.string.export_error_password_not_valid)
        }
    }

    //endregion
}
