package com.concordium.wallet.ui.walletconnect.proof.of.identity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.databinding.FragmentProofOfIdentityStatusBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel

class ProofOfIdentityStatusFragment : BaseFragment() {

    private var _binding: FragmentProofOfIdentityStatusBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WalletConnectViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProofOfIdentityStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
       // initViews()
        //initObservers()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}