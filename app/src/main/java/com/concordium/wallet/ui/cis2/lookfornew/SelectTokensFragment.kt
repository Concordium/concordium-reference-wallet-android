package com.concordium.wallet.ui.cis2.lookfornew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.FragmentDialogSelectTokensBinding
import com.concordium.wallet.ui.cis2.TokensAddAdapter
import com.concordium.wallet.ui.cis2.TokensBaseFragment
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.ui.cis2.TokensViewModel.Companion.TOKEN_DATA

class SelectTokensFragment : TokensBaseFragment() {
    private var _binding: FragmentDialogSelectTokensBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: TokensViewModel
    private lateinit var tokensAddAdapter: TokensAddAdapter
    private var firstTime = true

    companion object {
        @JvmStatic
        fun newInstance(viewModel: TokensViewModel) = SelectTokensFragment().apply {
            arguments = Bundle().apply {
                putSerializable(TOKEN_DATA, viewModel.tokenData)
            }
            _viewModel = viewModel
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialogSelectTokensBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        initObservers()
    }

    override fun onResume() {
        super.onResume()

        if (!firstTime)
            tokensAddAdapter.notifyDataSetChanged()
        firstTime = false
        _viewModel.hasExistingTokens()
        binding.nonSelected.visibility = View.INVISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.tokensFound.layoutManager = LinearLayoutManager(activity)
        tokensAddAdapter =
            TokensAddAdapter(requireActivity(), showCheckBox = true, dataSet = arrayOf())
        tokensAddAdapter.also { binding.tokensFound.adapter = it }
        tokensAddAdapter.dataSet = _viewModel.tokens.toTypedArray()

        binding.tokensFound.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (_viewModel.tokens.size > 0 && visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0 && totalItemCount > 3) {
                    _viewModel.lookForTokens(from = _viewModel.tokens[_viewModel.tokens.size - 1].id)
                }
            }
        })

        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val currentFilter = query?.uppercase() ?: ""
                tokensAddAdapter.dataSet = _viewModel.tokens.filter {
                    it.token.uppercase() == currentFilter
                }.toTypedArray()
                tokensAddAdapter.notifyDataSetChanged()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    tokensAddAdapter.dataSet = _viewModel.tokens.toTypedArray()
                    tokensAddAdapter.notifyDataSetChanged()
                }
                return false
            }
        })

        tokensAddAdapter.setTokenClickListener(object : TokensAddAdapter.TokenClickListener {
            override fun onRowClick(token: Token) {
                _viewModel.chooseTokenInfo.postValue(token)
                _viewModel.stepPage(1)
            }

            override fun onCheckBoxClick(token: Token) {
                _viewModel.toggleNewToken(token)
            }
        })

        binding.back.setOnClickListener {

            binding.search.setQuery("", false)
            _viewModel.stepPage(-1)
        }

        binding.updateWithTokens.setOnClickListener {
            _viewModel.updateWithSelectedTokens()
        }
    }

    private fun initObservers() {
        _viewModel.lookForTokens.observe(viewLifecycleOwner) {
            tokensAddAdapter.dataSet = _viewModel.tokens.toTypedArray()
            tokensAddAdapter.notifyDataSetChanged()
        }
        _viewModel.tokenDetails.observe(viewLifecycleOwner) {
            tokensAddAdapter.dataSet = _viewModel.tokens.toTypedArray()
            tokensAddAdapter.notifyDataSetChanged()
        }
        _viewModel.hasExistingAccountContract.observe(viewLifecycleOwner) { hasExistingAccountContract ->
            if (hasExistingAccountContract)
                binding.updateWithTokens.text = getString(R.string.cis_update_tokens)
            else
                binding.updateWithTokens.text = getString(R.string.cis_add_tokens)
            binding.updateWithTokens.isEnabled = true
        }
        _viewModel.nonSelected.observe(viewLifecycleOwner) { nonSelected ->
            if (nonSelected)
                binding.nonSelected.visibility = View.VISIBLE
            else
                binding.nonSelected.visibility = View.INVISIBLE
        }
    }
}
