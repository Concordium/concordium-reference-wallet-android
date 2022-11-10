package com.concordium.wallet.ui.walletconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.databinding.FragmentWalletConnectPairBinding
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA

class WalletConnectPairFragment : WalletConnectBaseFragment() {
    private var _binding: FragmentWalletConnectPairBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel

    companion object {
        @JvmStatic
        fun newInstance(viewModel: WalletConnectViewModel) = WalletConnectPairFragment().apply {
            arguments = Bundle().apply {
                putSerializable(WALLET_CONNECT_DATA, viewModel.walletConnectData)
            }
            _viewModel = viewModel
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWalletConnectPairBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        _viewModel.pair()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.accountName.text = _viewModel.walletConnectData.account?.name ?: ""
        binding.connect.setOnClickListener {
            binding.connect.isEnabled = false
            _viewModel.connect.postValue(true)
        }
        binding.decline.setOnClickListener {
            binding.decline.isEnabled = false
            _viewModel.rejectSession()
            _viewModel.decline.postValue(true)
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
                binding.progress.visibility = View.GONE
                binding.preparingConnection.visibility = View.GONE
                binding.connect.isEnabled = true
            }
        }
    }
}
