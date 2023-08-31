package com.concordium.wallet.ui.walletconnect

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.data.walletconnect.Payload
import com.concordium.wallet.databinding.FragmentWalletConnectTransactionBinding
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA

class WalletConnectTransactionFragment : WalletConnectBaseFragment() {
    private var _binding: FragmentWalletConnectTransactionBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel

    companion object {
        @JvmStatic
        fun newInstance(viewModel: WalletConnectViewModel) =
            WalletConnectTransactionFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(WALLET_CONNECT_DATA, viewModel.walletConnectData)
                }
                _viewModel = viewModel
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletConnectTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()

        _viewModel.prettyPrintJson()
        _viewModel.loadTransactionFee()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetTextI18n")
    private fun initViews() {
        _viewModel.walletConnectData.account?.let { account ->
            binding.atDisposal.text = CurrencyUtil.formatGTU(
                account.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance),
                true
            )
            binding.accountToSendFrom.text = account.address
        }


        _viewModel.binder?.getSessionRequestParams().let { requestParams ->
            if (requestParams == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.wallet_connect_transaction_parsing_error),
                    Toast.LENGTH_SHORT
                ).show()
                _viewModel.binder?.respondError("User reject")
                _viewModel.reject.postValue(true)
            } else {

                requestParams.parsePayload()?.let { payload ->
                    when (payload) {
                        is Payload.ContractUpdateTransaction -> {
                            setComplexPayloadVisibility()
                            binding.amount.text = CurrencyUtil.formatGTU(
                                payload.amount, true
                            )
                            payload.address.let {
                                binding.contractAddress.text = "${it.index} (${it.subIndex})"
                            }
                            binding.contractFeature.text = payload.receiveName
                        }

                        is Payload.AccountTransaction -> {
                            setSimplePayloadVisibility()
                            binding.amount.text = CurrencyUtil.formatGTU(
                                payload.amount, true
                            )
                            binding.receiverAddress.text = payload.toAddress
                        }
                    }
                }
            }
        }

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

    private fun setComplexPayloadVisibility() {
        binding.apply {
            contractTitle.visibility = View.VISIBLE
            contractAddress.visibility = View.VISIBLE
            contractFunctionTitle.visibility = View.VISIBLE
            contractFeature.visibility = View.VISIBLE
            maxEnergyAllowedTitle.visibility = View.VISIBLE
            maxEnergyAllowed.visibility = View.VISIBLE

            receiverTitle.visibility = View.GONE
            receiverAddress.visibility = View.GONE
        }
    }

    private fun setSimplePayloadVisibility() {
        binding.apply {
            contractTitle.visibility = View.GONE
            contractAddress.visibility = View.GONE
            contractFunctionTitle.visibility = View.GONE
            contractFeature.visibility = View.GONE
            maxEnergyAllowedTitle.visibility = View.GONE
            maxEnergyAllowed.visibility = View.GONE

            receiverTitle.visibility = View.VISIBLE
            receiverAddress.visibility = View.VISIBLE
        }
    }

    private fun initObservers() {
        _viewModel.transactionFee.observe(viewLifecycleOwner) { fee ->
            binding.estimatedTransactionFee.text = getString(
                R.string.wallet_connect_transaction_estimated_transaction_fee,
                CurrencyUtil.formatGTU(fee)
            )
            binding.maxEnergyAllowed.text = "${_viewModel.walletConnectData.energy} NRG"
            if (_viewModel.hasEnoughFunds())
                binding.submit.isEnabled = true
            else {
                binding.insufficient.visibility = View.VISIBLE
                binding.atDisposalTitle.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_pink
                    )
                )
                binding.atDisposal.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_pink
                    )
                )
                binding.amountTitle.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_pink
                    )
                )
                binding.amount.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_pink
                    )
                )
                binding.estimatedTransactionFee.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_pink
                    )
                )
            }
        }
        _viewModel.errorInt.observe(viewLifecycleOwner) {
            binding.submit.isEnabled = true
        }
        _viewModel.jsonPretty.observe(viewLifecycleOwner) { jsonPretty ->
            if (jsonPretty.isEmpty()) {
                binding.parametersTitle.text =
                    getString(R.string.wallet_connect_transaction_no_parameters)
                binding.parameters.visibility = View.GONE

            } else {
                binding.parametersTitle.text =
                    getString(R.string.wallet_connect_transaction_parameters)
                binding.parameters.visibility = View.VISIBLE
                binding.parameters.text = jsonPretty
            }
        }
    }
}
