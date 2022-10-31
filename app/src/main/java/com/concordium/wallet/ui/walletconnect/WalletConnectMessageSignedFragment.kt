package com.concordium.wallet.ui.walletconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.concordium.wallet.databinding.FragmentWalletConnectMessageSignedBinding

class WalletConnectMessageSignedFragment : Fragment() {
    private var _binding: FragmentWalletConnectMessageSignedBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel

    companion object {
        @JvmStatic
        fun newInstance(viewModel: WalletConnectViewModel) = WalletConnectMessageSignedFragment().apply {
            arguments = Bundle().apply {
                putSerializable(WalletConnectViewModel.WALLET_CONNECT_DATA, viewModel.walletConnectData)
            }
            _viewModel = viewModel
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWalletConnectMessageSignedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.okay.setOnClickListener {
            binding.okay.isEnabled = false
            _viewModel.messagedSignedOkay.postValue(true)
        }
    }
}
