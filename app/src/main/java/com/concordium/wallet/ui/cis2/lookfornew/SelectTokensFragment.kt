package com.concordium.wallet.ui.cis2.lookfornew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.FragmentDialogSelectTokensBinding
import com.concordium.wallet.ui.cis2.TokensListAdapter
import com.concordium.wallet.ui.cis2.TokensViewModel

class SelectTokensFragment : Fragment() {
    private var _binding: FragmentDialogSelectTokensBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TokensViewModel by activityViewModels()
    private lateinit var tokensListAdapter: TokensListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDialogSelectTokensBinding.inflate(inflater, container, false)
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

    private fun initViews() {
        tokensListAdapter = TokensListAdapter(requireContext(), arrayOf(), true)
        tokensListAdapter.also { binding.tokensFound.adapter = it }

        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                tokensListAdapter.arrayList = viewModel.newTokens.value!!.filter {
                    it.name.uppercase().contains(query?.uppercase() ?: "")
                }.toTypedArray()
                tokensListAdapter.notifyDataSetChanged()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                tokensListAdapter.arrayList = viewModel.newTokens.value!!.filter {
                    it.name.uppercase().contains(newText?.uppercase() ?: "")
                }.toTypedArray()
                tokensListAdapter.notifyDataSetChanged()
                return false
            }
        })

        tokensListAdapter.setTokenClickListener(object : TokensListAdapter.TokenClickListener {
            override fun onRowClick(token: Token) {
                viewModel.stepPage(1)
            }
            override fun onCheckBoxClick(token: Token) {
                viewModel.toggleNewToken(token)
            }
        })

        binding.back.setOnClickListener {
            viewModel.stepPage(-1)
        }

        binding.addTokens.setOnClickListener {
            viewModel.addSelectedTokens()
        }
    }

    private fun initObservers() {
        viewModel.newTokens.observe(viewLifecycleOwner) { tokens ->
            tokensListAdapter.arrayList = tokens.toTypedArray()
            tokensListAdapter.notifyDataSetChanged()
        }
    }
}
