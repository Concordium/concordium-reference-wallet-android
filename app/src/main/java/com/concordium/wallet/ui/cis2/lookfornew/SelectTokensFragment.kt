package com.concordium.wallet.ui.cis2.lookfornew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.FragmentDialogSelectTokensBinding
import com.concordium.wallet.ui.cis2.TokensBaseFragment
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.ui.cis2.TokensViewModel.Companion.TOKEN_DATA

class SelectTokensFragment : TokensBaseFragment() {
    private var _binding: FragmentDialogSelectTokensBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: TokensViewModel
    private lateinit var tokensListAdapter: SelectTokensAdapter
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
            tokensListAdapter.notifyDataSetChanged()
        firstTime = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.tokensFound.layoutManager = LinearLayoutManager(activity)

        binding.tokensFound.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0) {
                    _viewModel.lookForTokens(from = _viewModel.tokens[_viewModel.tokens.size - 1].id)
                }
            }
        })

        tokensListAdapter = SelectTokensAdapter(requireActivity(), arrayOf())
        tokensListAdapter.also { binding.tokensFound.adapter = it }

        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                tokensListAdapter.dataSet = _viewModel.tokens.filter {
                    it.token.uppercase().contains(query?.uppercase() ?: "")
                }.toTypedArray()
                tokensListAdapter.notifyDataSetChanged()
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                tokensListAdapter.dataSet = _viewModel.tokens.filter {
                    it.token.uppercase().contains(newText?.uppercase() ?: "")
                }.toTypedArray()
                tokensListAdapter.notifyDataSetChanged()
                return false
            }
        })

        tokensListAdapter.setTokenClickListener(object : SelectTokensAdapter.TokenClickListener {
            override fun onRowClick(token: Token) {
                _viewModel.stepPage(1)
            }
            override fun onCheckBoxClick(token: Token) {
                _viewModel.toggleNewToken(token)
            }
        })

        binding.back.setOnClickListener {
            _viewModel.stepPage(-1)
        }

        binding.addTokens.setOnClickListener {
            _viewModel.addSelectedTokens()
        }
    }

    private fun initObservers() {
        _viewModel.waitingTokens.observe(viewLifecycleOwner) { waiting ->
            if (!waiting) {
                println("LC -> ${_viewModel.tokens}")
                tokensListAdapter.dataSet = _viewModel.tokens.toTypedArray()
                tokensListAdapter.notifyDataSetChanged()
            }
        }
        _viewModel.tokenDetails.observe(viewLifecycleOwner) { tokenId ->
            tokensListAdapter.notifyItemChanged(_viewModel.findTokenPositionById(tokenId))
        }
    }
}
