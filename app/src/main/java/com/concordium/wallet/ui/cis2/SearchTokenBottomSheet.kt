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

class SearchTokenBottomSheet : BaseBottomSheetDialogFragment() {
    private var _binding: DialogSearchTokenBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokensAddAdapter: TokensAddAdapter
    private lateinit var _viewModel: SendTokenViewModel

    companion object {
        @JvmStatic
        fun newInstance(viewModel: SendTokenViewModel) = SearchTokenBottomSheet().apply {
            arguments = Bundle().apply {
                putSerializable(SEND_TOKEN_DATA, viewModel.sendTokenData)
            }
            _viewModel = viewModel
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
        _viewModel.loadTokens()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.tokensFound.layoutManager = LinearLayoutManager(activity)
        tokensAddAdapter = TokensAddAdapter(requireActivity(), showCheckBox = true, dataSet = arrayOf())
        tokensAddAdapter.also { binding.tokensFound.adapter = it }

        tokensAddAdapter.setTokenClickListener(object : TokensAddAdapter.TokenClickListener {
            override fun onRowClick(token: Token) {
                _viewModel.chooseToken.postValue(token)
            }
            override fun onCheckBoxClick(token: Token) {
            }
        })

        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                tokensAddAdapter.dataSet = _viewModel.tokens.value!!.filter { it.token.uppercase().contains(query?.uppercase() ?: "") }.toTypedArray()
                tokensAddAdapter.notifyDataSetChanged()
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                tokensAddAdapter.dataSet = _viewModel.tokens.value!!.filter { it.token.uppercase().contains(newText?.uppercase() ?: "") }.toTypedArray()
                tokensAddAdapter.notifyDataSetChanged()
                return false
            }
        })
    }

    private fun initObservers() {
        _viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
        _viewModel.tokens.observe(this) { tokens ->
            tokensAddAdapter.dataSet = tokens.toTypedArray()
            tokensAddAdapter.notifyDataSetChanged()
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.tokensFound.visibility = if (waiting) View.GONE else View.VISIBLE
    }
}
