package com.concordium.wallet.ui.walletconnect

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentWalletConnectMessageBinding
import com.concordium.wallet.databinding.FragmentWalletConnectTransactionBinding
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

class WalletConnectMessageFragment : WalletConnectBaseFragment() {
    private var _binding: FragmentWalletConnectMessageBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel

    companion object {
        @JvmStatic
        fun newInstance(viewModel: WalletConnectViewModel) = WalletConnectMessageFragment().apply {
            arguments = Bundle().apply {
                putSerializable(WALLET_CONNECT_DATA, viewModel.walletConnectData)
            }
            _viewModel = viewModel
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWalletConnectMessageBinding.inflate(inflater, container, false)
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

    @SuppressLint("SetTextI18n")
    private fun initViews() {
        binding.messageText.text = "TEST"
        binding.reject.setOnClickListener {
            binding.reject.isEnabled = false
            _viewModel.binder?.rejectTransaction()
            _viewModel.reject.postValue(true)
        }
        binding.sign.setOnClickListener {
            binding.sign.isEnabled = false
            _viewModel.prepareMessage()
        }
    }

    private fun initObservers() {
        _viewModel.errorInt.observe(viewLifecycleOwner) {
            binding.sign.isEnabled = true
        }
    }
}
