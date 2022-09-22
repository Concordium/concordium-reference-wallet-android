package com.concordium.wallet.ui.walletconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentWalletConnectTransactionBinding
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA

class WalletConnectTransactionFragment : WalletConnectBaseFragment() {
    private var _binding: FragmentWalletConnectTransactionBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel

    companion object {
        @JvmStatic
        fun newInstance(viewModel: WalletConnectViewModel, walletConnectData: WalletConnectData) = WalletConnectTransactionFragment().apply {
            arguments = Bundle().apply {
                putSerializable(WALLET_CONNECT_DATA, walletConnectData)
            }
            _viewModel = viewModel
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWalletConnectTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        _viewModel.approve()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.accountToSendFrom.text = "test 1"
        binding.amount.text = "test 2"
        binding.contractAddress.text = "test 3"
        binding.parameters.text = "test 4"
        binding.estimatedTransactionFee.text = getString(R.string.wallet_connect_transaction_estimated_transaction_fee, CurrencyUtil.formatGTU(12345))
        binding.reject.setOnClickListener {

        }
        binding.submit.setOnClickListener {

        }
    }

    private fun initObservers() {
        /*
        _viewModel.connectStatus.observe(viewLifecycleOwner) { isConnected ->
            binding.waitForActions.visibility = View.GONE
        }
        }*/
    }
}
