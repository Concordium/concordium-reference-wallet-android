package com.concordium.wallet.ui.walletconnect.proof.of.identity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.databinding.FragmentProofOfIdentityPromptBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel

class ProofOfIdentityPromptFragment : BaseFragment() {

    private var _binding: FragmentProofOfIdentityPromptBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WalletConnectViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProofOfIdentityPromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
    }

    private fun initViews() {
        binding.accept.setOnClickListener {

        }
        binding.reject.setOnClickListener {

        }
    }

    private fun initObservers() {
        viewModel.proofOfIdentityRequest.observe(viewLifecycleOwner){
            //if(it.statement?.isNotEmpty() == true){
                viewModel.validateProofOfIdentity(it)
          //  }

        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}