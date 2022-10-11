package com.concordium.wallet.ui.cis2

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.DialogLookForNewTokensBinding
import com.concordium.wallet.ui.base.BaseBottomSheetDialogFragment
import com.concordium.wallet.util.KeyboardUtil
import org.bouncycastle.jcajce.provider.asymmetric.GOST

class LookForNewTokensBottomSheet : BaseBottomSheetDialogFragment() {
    private var _binding: DialogLookForNewTokensBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TokensViewModel by activityViewModels()
    private lateinit var tokensListAdapter: TokensListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogLookForNewTokensBinding.inflate(inflater, container, false)
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

        binding.title.text = getString(R.string.cis_find_tokens_title)

        binding.look.setOnClickListener {
            look()
        }

        binding.contractAddress.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                look()
                KeyboardUtil.hideKeyboard(requireActivity())
                true
            } else {
                false
            }
        }

        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                tokensListAdapter.arrayList = viewModel.newTokens.value!!.filter { it.name.uppercase().contains(query?.uppercase() ?: "") }.toTypedArray()
                tokensListAdapter.notifyDataSetChanged()
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                tokensListAdapter.arrayList = viewModel.newTokens.value!!.filter { it.name.uppercase().contains(newText?.uppercase() ?: "") }.toTypedArray()
                tokensListAdapter.notifyDataSetChanged()
                return false
            }
        })

        tokensListAdapter.setTokenClickListener { token ->
            viewModel.toggleNewToken(token)
        }
    }

    private fun initObservers() {
        viewModel.waitingNewTokens.observe(this) { waiting ->
            showWaiting(waiting)
        }
        viewModel.newTokens.observe(this) { tokens ->
            handleLookup(tokens)
        }
    }

    private fun look() {
        if (binding.contractAddress.text.isNotBlank())
            viewModel.lookForNewTokens(binding.contractAddress.text.toString())
    }

    private fun handleLookup(tokens: List<Token>) {
        tokensListAdapter.arrayList = tokens.toTypedArray()
        tokensListAdapter.notifyDataSetChanged()
        binding.look.isEnabled = true
        if (tokens.isNotEmpty()) {
            binding.error.visibility = View.GONE
            binding.title.text = getString(R.string.cis_select_tokens_title)
            binding.contractAddress.visibility = View.GONE
            binding.search.visibility = View.VISIBLE
            binding.tokensFound.visibility = View.VISIBLE
            binding.look.text = getString(R.string.cis_add_tokens)
            binding.look.setOnClickListener {
                viewModel.addSelectedTokens()
            }
            binding.contractAddress.onFocusChangeListener = null
        } else {
            binding.error.visibility = View.VISIBLE
            binding.title.text = getString(R.string.cis_find_tokens_title)
            binding.contractAddress.isEnabled = true
            binding.contractAddress.setTextColor(activity?.getColor(R.color.text_pink) ?: Color.RED)
            binding.contractAddress.setBackgroundResource(R.drawable.rounded_pink)
            binding.contractAddress.setOnFocusChangeListener { _, _ ->
                binding.contractAddress.setTextColor(activity?.getColor(R.color.text_blue) ?: Color.BLUE)
                binding.contractAddress.setBackgroundResource(R.drawable.rounded_light_grey)
                binding.error.visibility = View.GONE
            }
        }
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.look.isEnabled = false
            binding.contractAddress.isEnabled = false
            binding.pending.visibility = View.VISIBLE
            binding.search.visibility = View.GONE
            binding.tokensFound.visibility = View.GONE
        } else {
            binding.pending.visibility = View.GONE
            binding.tokensFound.visibility = View.VISIBLE
        }
    }
}
