package com.concordium.wallet.ui.identity.identitycreate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentIdentityCreateIdentityNameBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.ValidationUtil

class IdentityCreateIdentityNameFragment : BaseFragment(R.string.identity_create_title) {
    private var _binding: FragmentIdentityCreateIdentityNameBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel: IdentityCreateViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIdentityCreateIdentityNameBinding.inflate(inflater, container, false)
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
        binding.identityNameEdittext.afterTextChanged { text ->
            binding.confirmButton.isEnabled = text.isNotEmpty()
        }

        binding.identityNameEdittext.setOnEditorActionListener { textView, actionId, _ ->
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
        if (!ValidationUtil.validateName(binding.identityNameEdittext.text.toString())) {
            binding.identityNameEdittext.error = getString(R.string.valid_special_chars_error_text)
            return
        }

        startActivity(Intent(requireContext(), IdentityProviderListActivity::class.java))
    }

    //endregion
}
