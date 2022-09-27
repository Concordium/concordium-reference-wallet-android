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
        binding.amount.text = "test 2"
        binding.contractAddress.text = "test 3"
        binding.parameters.text = prettyPrintJson()
        binding.estimatedTransactionFee.text = getString(R.string.wallet_connect_transaction_estimated_transaction_fee, CurrencyUtil.formatGTU(1234567890))
        binding.reject.setOnClickListener {
            _viewModel.reject.postValue(true)
        }
        binding.submit.setOnClickListener {
            _viewModel.prepareTransaction()
        }
    }

    private fun initObservers() {

    }

    private fun prettyPrintJson(): String {
        val jsonString = "{\"profile\":{\"uid\":\"492775\",\"firstName\":\"Anders 223\",\"lastName\":\"And 223\",\"address1\":\"Bla2\",\"address2\":null,\"zipcode\":\"5000\",\"city\":\"Odense C\",\"country\":\"Danmark\",\"phone\":null,\"cellphone\":\"+4587654321\",\"workphone\":null,\"email\":\"andersand@gmail.com\",\"alternative_email\":null,\"created\":\"2021-10-25T09:40:43+02:00\",\"lat\":\"0\",\"lng\":\"0\",\"externalId\":\"\",\"newsmail\":\"1\",\"cardno\":\"\",\"paymentMethods\":{\"methods\":{\"1\":{\"txt\":\"Dankort\",\"method\":\"epay\",\"subtxt\":\"\",\"type\":\"1\",\"collection\":1},\"16\":{\"txt\":\"Visa \\\\/ Visa Electron\",\"method\":\"epay\",\"subtxt\":\"\",\"type\":\"3\",\"collection\":1},\"32\":{\"txt\":\"Mastercard\",\"method\":\"epay\",\"subtxt\":\"\",\"type\":\"4\",\"collection\":1},\"64\":{\"txt\":\"Maestro\",\"method\":\"epay\",\"subtxt\":\"\",\"type\":\"7\",\"collection\":1},\"128\":{\"txt\":\"JCP\",\"method\":\"epay\",\"subtxt\":\"\",\"type\":\"6\",\"collection\":1},\"256\":{\"txt\":\"MobilePay\",\"method\":\"epay\",\"subtxt\":\"\",\"type\":\"29\",\"collection\":4}},\"default\":1},\"admin\":0,\"businesscard\":{\"accountno\":\"\",\"note\":\"\"}},\"eid\":200,\"uid\":492775,\"tid\":\"af6cdec4-9fba-4962-90d2-850276d745a8\"}"
        val json = JsonParser.parseString(jsonString).asJsonObject
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(json)
    }
}
