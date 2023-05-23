package com.concordium.wallet.ui.cis2.lookfornew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentDialogContractAddressBinding
import com.concordium.wallet.ui.cis2.TokensBaseFragment
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.util.KeyboardUtil

class ContractAddressFragment : TokensBaseFragment() {
    private var _binding: FragmentDialogContractAddressBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: TokensViewModel

    companion object {
        @JvmStatic
        fun newInstance(viewModel: TokensViewModel) = ContractAddressFragment().apply {
            arguments = Bundle().apply {
                putSerializable(TokensViewModel.TOKEN_DATA, viewModel.tokenData)
            }
            _viewModel = viewModel
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDialogContractAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.look.setOnClickListener {
            lookForTokens()
        }

        binding.contractAddress.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                showOrHideError(TokensViewModel.TOKENS_OK)
        }

        binding.contractAddress.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                lookForTokens()
                KeyboardUtil.hideKeyboard(requireActivity())
                true
            } else {
                false
            }
        }
    }

    private fun initObservers() {
        _viewModel.lookForTokens.observe(viewLifecycleOwner) { result ->
            showWaiting(false)
            showOrHideError(result)
        }
    }

    private fun lookForTokens() {
        showWaiting(true)
        _viewModel.tokenData.contractIndex = binding.contractAddress.text.toString()
        if (binding.contractAddress.text.isNotBlank()) {
            _viewModel.tokens.clear()
            _viewModel.lookForTokens(_viewModel.tokenData.account!!.address)
        }
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.look.isEnabled = false
            binding.contractAddress.isEnabled = false
            binding.pending.visibility = View.VISIBLE
        } else {
            binding.look.isEnabled = true
            binding.contractAddress.isEnabled = true
            binding.pending.visibility = View.GONE
        }
    }

    private fun showOrHideError(result: Int) {
        when (result) {
            TokensViewModel.TOKENS_NOT_LOADED,
            TokensViewModel.TOKENS_OK -> {
                binding.error.visibility = View.GONE
                binding.contractAddress.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_blue))
                binding.contractAddress.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.text_blue))
                binding.contractAddress.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_light_grey)
            }
            else -> {
                binding.error.visibility = View.VISIBLE
                binding.contractAddress.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_pink))
                binding.contractAddress.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.text_pink))
                binding.contractAddress.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_cardview_border_pink)
                if (result == TokensViewModel.TOKENS_EMPTY)
                    binding.error.text = getString(R.string.cis_find_tokens_none)
                else
                    binding.error.text = getString(R.string.cis_find_tokens_error)
            }
        }
    }
}
