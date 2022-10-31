package com.concordium.wallet.ui.walletconnect

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentWalletConnectTransactionBinding
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

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
        _viewModel.loadTransactionFee()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetTextI18n")
    private fun initViews() {
        _viewModel.walletConnectData.account?.let { account ->
            binding.atDisposal.text = CurrencyUtil.formatGTU(account.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance), true)
            binding.accountToSendFrom.text = "${account.name}\n\n${account.address}"
        }
        binding.amount.text = CurrencyUtil.formatGTU(_viewModel.binder?.getSessionRequestParams()?.parsePayload()?.amount?.microGtuAmount ?: "", true)
        binding.contractAddress.text = "${_viewModel.binder?.getSessionRequestParams()?.parsePayload()?.contractAddress?.index} ${_viewModel.binder?.getSessionRequestParams()?.parsePayload()?.contractAddress?.subindex}"
        binding.parameters.text = prettyPrintJson()
        binding.reject.setOnClickListener {
            binding.reject.isEnabled = false
            _viewModel.binder?.rejectTransaction()
            _viewModel.reject.postValue(true)
        }
        binding.submit.setOnClickListener {
            binding.submit.isEnabled = false
            _viewModel.prepareTransaction()
        }
    }

    private fun initObservers() {
        _viewModel.transactionFee.observe(viewLifecycleOwner) { fee ->
            binding.estimatedTransactionFee.text = getString(R.string.wallet_connect_transaction_estimated_transaction_fee, CurrencyUtil.formatGTU(fee))
            binding.submit.isEnabled = true
        }
        _viewModel.errorInt.observe(viewLifecycleOwner) {
            binding.submit.isEnabled = true
        }
    }

    private fun prettyPrintJson(): String {
        _viewModel.binder?.getSessionRequestParamsAsString()?.let { jsonString ->
            val json = JsonParser.parseString(jsonString).asJsonObject
            val gson = GsonBuilder().setPrettyPrinting().create()
            return gson.toJson(json)
        }
        return ""
    }
}
