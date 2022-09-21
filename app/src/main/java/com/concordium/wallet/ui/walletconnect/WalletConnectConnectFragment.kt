package com.concordium.wallet.ui.walletconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.databinding.FragmentWalletConnectConnectBinding
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA

class WalletConnectConnectFragment : WalletConnectBaseFragment() {
    private var _binding: FragmentWalletConnectConnectBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel

    companion object {
        @JvmStatic
        fun newInstance(viewModel: WalletConnectViewModel, walletConnectData: WalletConnectData) = WalletConnectConnectFragment().apply {
            arguments = Bundle().apply {
                putSerializable(WALLET_CONNECT_DATA, walletConnectData)
            }
            _viewModel = viewModel
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWalletConnectConnectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        _viewModel.pairWalletConnect()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.accountName.text = _viewModel.walletConnectData.account?.name ?: ""
        binding.connect.setOnClickListener {
            _viewModel.approve()
        }
        binding.decline.setOnClickListener {
            _viewModel.disconnectWalletConnect()
        }
    }

    private fun initObservers() {
        _viewModel.serviceName.observe(viewLifecycleOwner) { serviceName ->
            serviceName?.let {
                binding.serviceName.text = serviceName
            }
        }
        _viewModel.permissions.observe(viewLifecycleOwner) { permissions ->
            permissions?.let {
                binding.servicePermissions.text = permissions.joinToString("\n") { it }
            }
        }
    }
}
