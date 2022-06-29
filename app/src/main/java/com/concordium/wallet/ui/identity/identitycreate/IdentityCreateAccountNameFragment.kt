package com.concordium.wallet.ui.identity.identitycreate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentIdentityCreateAccountNameBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.ValidationUtil

class IdentityCreateAccountNameFragment : BaseFragment(R.string.identity_create_title) {
    private var _binding: FragmentIdentityCreateAccountNameBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel: IdentityCreateViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        sharedViewModel.initialize()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentIdentityCreateAccountNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle)
        initializeViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        sharedViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[IdentityCreateViewModel::class.java]
    }

    private fun initializeViews() {
        binding.confirmButton.isEnabled = false
        binding.confirmButton.setOnClickListener {
            gotoIdentityName()
        }
        binding.accountNameEdittext.afterTextChanged { text ->
            binding.confirmButton.isEnabled = !text.isNullOrEmpty()
        }

        binding.accountNameEdittext.setOnEditorActionListener { textView, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (textView.text.isNotEmpty())
                        gotoIdentityName()
                    true
                }
                else -> false
            }
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoIdentityName() {
        if (!ValidationUtil.validateName(binding.accountNameEdittext.text.toString())) {
            binding.accountNameEdittext.error = getString(R.string.valid_special_chars_error_text)
            return
        }
        sharedViewModel.customAccountName = binding.accountNameEdittext.text.toString().trim()
        findNavController().navigate(IdentityCreateAccountNameFragmentDirections.actionNavIdentityCreateAccountNameToNavIdentityCreateIdentityName())
    }

    //endregion
}
