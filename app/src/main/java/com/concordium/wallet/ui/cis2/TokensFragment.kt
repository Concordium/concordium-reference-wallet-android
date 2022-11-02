package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.FragmentTokensBinding

class TokensFragment : Fragment() {
    private var _binding: FragmentTokensBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: TokensViewModel
    private lateinit var tokensListAdapter: TokensListAdapter
    private var _accountAddress = ""
    private var _isFungible = true

    companion object {
        @JvmStatic
        fun newInstance(viewModel: TokensViewModel, accountAddress: String, isFungible: Boolean) = TokensFragment().apply {
            _viewModel = viewModel
            _accountAddress = accountAddress
            _isFungible = isFungible
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTokensBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        _viewModel.loadTokens(_isFungible)
    }

    private fun initViews() {
        tokensListAdapter = TokensListAdapter(requireContext(), arrayOf(), false)
        tokensListAdapter.setTokenClickListener(object : TokensListAdapter.TokenClickListener {
            override fun onRowClick(token: Token) {
                _viewModel.chooseToken.postValue(token)
            }
            override fun onCheckBoxClick(token: Token) {
            }
        })
        tokensListAdapter.also { binding.listTokens.adapter = it }
    }

    private fun initObservers() {
        /*
        _viewModel.tokens.observe(viewLifecycleOwner) { tokens ->
            tokensListAdapter.arrayList = tokens.toTypedArray()
            tokensListAdapter.notifyDataSetChanged()
        }*/
    }
}
