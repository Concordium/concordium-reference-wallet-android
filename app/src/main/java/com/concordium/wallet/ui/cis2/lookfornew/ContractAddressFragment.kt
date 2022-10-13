package com.concordium.wallet.ui.cis2.lookfornew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.databinding.FragmentDialogContractAddressBinding
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.util.KeyboardUtil

class ContractAddressFragment : Fragment() {
    private var _binding: FragmentDialogContractAddressBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TokensViewModel by activityViewModels()

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
            look()
        }

        binding.contractAddress.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                look()
                KeyboardUtil.hideKeyboard(requireActivity())
                true
            } else {
                false
            }
        }
    }

    private fun initObservers() {
        viewModel.waitingNewTokens.observe(viewLifecycleOwner) { waiting ->
            showWaiting(waiting)
        }
    }

    private fun look() {
        if (binding.contractAddress.text.isNotBlank()) {
            viewModel.lookForNewTokens(binding.contractAddress.text.toString())
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
}
