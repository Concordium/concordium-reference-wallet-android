package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.FragmentTokensBinding

class TokensFragment : Fragment() {
    private var _binding: FragmentTokensBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: TokensViewModel
    private lateinit var tokensAccountDetailsAdapter: TokensAccountDetailsAdapter
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
        _viewModel.loadTokens(_accountAddress, _isFungible)
    }

    private fun initViews() {
        binding.tokensFound.layoutManager = LinearLayoutManager(activity)
        tokensAccountDetailsAdapter = TokensAccountDetailsAdapter(requireContext(), _isFungible, arrayOf())
        tokensAccountDetailsAdapter.also { binding.tokensFound.adapter = it }

        tokensAccountDetailsAdapter.setTokenClickListener(object : TokensAccountDetailsAdapter.TokenClickListener {
            override fun onRowClick(token: Token) {
                _viewModel.chooseToken.postValue(token)
            }
            override fun onCheckBoxClick(token: Token) {
            }
        })
    }

    private fun initObservers() {
        _viewModel.waiting.observe(viewLifecycleOwner) {
            if (!_isFungible) {
                if (_viewModel.tokens.isEmpty())
                    binding.noItems.visibility = View.VISIBLE
                else
                    binding.noItems.visibility = View.GONE
            }
            tokensAccountDetailsAdapter.dataSet = _viewModel.tokens.toTypedArray()
            tokensAccountDetailsAdapter.notifyDataSetChanged()
        }
        _viewModel.tokenDetails.observe(viewLifecycleOwner) {
            tokensAccountDetailsAdapter.notifyDataSetChanged()
        }
    }
}