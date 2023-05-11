package com.concordium.wallet.ui.walletconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.databinding.FragmentWalletConnectChooseAccountBinding
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA

class WalletConnectChooseAccountFragment : WalletConnectBaseFragment() {
    private var _binding: FragmentWalletConnectChooseAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel
    private lateinit var chooseAccountAdapter: ChooseAccountListAdapter

    companion object {
        @JvmStatic
        fun newInstance(viewModel: WalletConnectViewModel) =
            WalletConnectChooseAccountFragment().apply {
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
        _binding = FragmentWalletConnectChooseAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        _viewModel.loadAccounts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        chooseAccountAdapter = ChooseAccountListAdapter(requireContext(), arrayOf())
        chooseAccountAdapter.setChooseAccountClickListener { accountWithIdentity ->
            _viewModel.chooseAccount.postValue(accountWithIdentity)
        }
        chooseAccountAdapter.also { binding.accountsListView.adapter = it }
    }

    private fun initObservers() {
        _viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
            chooseAccountAdapter.arrayList = accounts.toTypedArray()
            chooseAccountAdapter.notifyDataSetChanged()
        }
    }
}
