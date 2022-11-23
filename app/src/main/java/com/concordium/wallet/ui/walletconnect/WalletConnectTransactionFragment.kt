package com.concordium.wallet.ui.walletconnect

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentWalletConnectTransactionBinding
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder

class WalletConnectTransactionFragment : WalletConnectBaseFragment() {
    private var _binding: FragmentWalletConnectTransactionBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel

    companion object {
        @JvmStatic
        fun newInstance(viewModel: WalletConnectViewModel) = WalletConnectTransactionFragment().apply {
            arguments = Bundle().apply {
                putSerializable(WALLET_CONNECT_DATA, viewModel.walletConnectData)
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
            val line1 = account.address.substring(0, account.address.length / 2)
            val line2 = account.address.substring(account.address.length / 2)
            binding.accountToSendFrom.text = "${account.name}\n\n$line1\n$line2"
        }
        binding.amount.text = CurrencyUtil.formatGTU(_viewModel.binder?.getSessionRequestParams()?.parsePayload()?.amount ?: "", true)
        binding.contractAddress.text = "${_viewModel.binder?.getSessionRequestParams()?.parsePayload()?.address?.index}"
        binding.parameters.text = prettyPrintJson()
        binding.reject.setOnClickListener {
            binding.reject.isEnabled = false
            _viewModel.binder?.respondError("User reject")
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
            if (_viewModel.hasEnoughFunds())
                binding.submit.isEnabled = true
            else {
                binding.insufficient.visibility = View.VISIBLE
                binding.atDisposalTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_pink))
                binding.atDisposal.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_pink))
                binding.amountTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_pink))
                binding.amount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_pink))
                binding.estimatedTransactionFee.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_pink))
            }
        }
        _viewModel.errorInt.observe(viewLifecycleOwner) {
            binding.submit.isEnabled = true
        }
    }

    private fun prettyPrintJson(): String {
        _viewModel.binder?.getSessionRequestParams()?.let { params ->
            params.payloadObj = params.parsePayload()
            params.payload = ""
            val strategy: ExclusionStrategy = object : ExclusionStrategy {
                override fun shouldSkipField(f: FieldAttributes): Boolean {
                    return f.name == "payload"
                }
                override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                    return false
                }
            }
            val gson = GsonBuilder().setPrettyPrinting().addSerializationExclusionStrategy(strategy).create()
            return gson.toJson(params).replace("payloadObj", "payload")
        }
        return ""
    }
}
