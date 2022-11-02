package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.DialogSearchTokenBinding
import com.concordium.wallet.ui.base.BaseBottomSheetDialogFragment
import com.concordium.wallet.util.KeyboardUtil

class SearchTokenBottomSheet : BaseBottomSheetDialogFragment() {
    private var _binding: DialogSearchTokenBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokensListAdapter: TokensListAdapter
    private val viewModel: SendTokenViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSearchTokenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        viewModel.loadTokens()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        tokensListAdapter = TokensListAdapter(requireContext(), arrayOf(), false)
        tokensListAdapter.setTokenClickListener(object : TokensListAdapter.TokenClickListener {
            override fun onRowClick(token: Token) {
                viewModel.chooseToken.postValue(token)
            }
            override fun onCheckBoxClick(token: Token) {
            }
        })
        tokensListAdapter.also { binding.tokensFound.adapter = it }
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                tokensListAdapter.arrayList = viewModel.tokens.value!!.filter { it.token.uppercase().contains(query?.uppercase() ?: "") }.toTypedArray()
                tokensListAdapter.notifyDataSetChanged()
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                tokensListAdapter.arrayList = viewModel.tokens.value!!.filter { it.token.uppercase().contains(newText?.uppercase() ?: "") }.toTypedArray()
                tokensListAdapter.notifyDataSetChanged()
                return false
            }
        })
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
        viewModel.tokens.observe(this) { tokens ->
            tokensListAdapter.arrayList = tokens.toTypedArray()
            tokensListAdapter.notifyDataSetChanged()
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.tokensFound.visibility = if (waiting) View.GONE else View.VISIBLE
    }
}
