package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.DialogSearchTokenBinding
import com.concordium.wallet.ui.base.BaseBottomSheetDialogFragment
import com.concordium.wallet.ui.cis2.SendTokenViewModel.Companion.SEND_TOKEN_DATA
import com.concordium.wallet.util.Log

class SearchTokenBottomSheet : BaseBottomSheetDialogFragment() {
    private var _binding: DialogSearchTokenBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokensAccountDetailsAdapter: TokensAccountDetailsAdapter
    private lateinit var _viewModel: SendTokenViewModel
    private lateinit var _viewModelTokens: TokensViewModel

    companion object {
        @JvmStatic
        fun newInstance(viewModel: SendTokenViewModel, viewModelTokens: TokensViewModel) = SearchTokenBottomSheet().apply {
            arguments = Bundle().apply {
                putSerializable(SEND_TOKEN_DATA, viewModel.sendTokenData)
            }
            _viewModel = viewModel
            _viewModelTokens = viewModelTokens
            _viewModelTokens.tokenData.account = _viewModel.sendTokenData.account
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSearchTokenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        _viewModel.loadTokens(_viewModel.sendTokenData.account?.address ?: "")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.tokensFound.layoutManager = LinearLayoutManager(activity)
        tokensAccountDetailsAdapter = TokensAccountDetailsAdapter(requireActivity(), isFungible = true, showManageInfo = false, dataSet = arrayOf())
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
        _viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
        _viewModel.tokens.observe(this) { tokens ->
            tokensAccountDetailsAdapter.dataSet = tokens.toTypedArray()
            tokensAccountDetailsAdapter.notifyDataSetChanged()
            _viewModelTokens.tokens = tokens as MutableList<Token>
            _viewModelTokens.loadTokensBalances()
        }
        _viewModelTokens.tokenBalances.observe(viewLifecycleOwner) {
            tokensAccountDetailsAdapter.dataSet = _viewModelTokens.tokens.toTypedArray()
            tokensAccountDetailsAdapter.notifyDataSetChanged()
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.tokensFound.visibility = if (waiting) View.GONE else View.VISIBLE
    }
}
