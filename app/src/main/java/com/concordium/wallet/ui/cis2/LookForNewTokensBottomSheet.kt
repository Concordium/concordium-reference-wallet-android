package com.concordium.wallet.ui.cis2

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogLookForNewTokensBinding
import com.concordium.wallet.ui.base.BaseBottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LookForNewTokensBottomSheet : BaseBottomSheetDialogFragment() {
    private var _binding: DialogLookForNewTokensBinding? = null
    private val binding get() = _binding!!

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
        binding.title.text = getString(R.string.cis_find_tokens_title)
        binding.look.setOnClickListener {
            look()
        }
        binding.contractAddress.setOnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                look()
                true
            } else {
                false
            }
        }
    }

    private fun initObservers() {

    }

    private fun look() {
        if (binding.contractAddress.text.isBlank())
            return

        binding.look.isEnabled = false
        binding.contractAddress.isEnabled = false
        binding.pending.visibility = View.VISIBLE
        binding.error.visibility = View.GONE
        CoroutineScope(Dispatchers.IO).launch {
            delay(2000)
            activity?.runOnUiThread {
                handleLookup()
            }
        }
    }

    private fun handleLookup() {
        val ok = true
        binding.look.isEnabled = true
        binding.pending.visibility = View.GONE
        if (ok) {
            binding.title.text = getString(R.string.cis_select_tokens_title)
            binding.contractAddress.visibility = View.GONE
            binding.search.visibility = View.VISIBLE
            binding.tokensFound.visibility = View.VISIBLE
            binding.look.text = getString(R.string.cis_add_tokens)
            binding.look.setOnClickListener {
                addTokens()
            }
        } else {
            binding.contractAddress.isEnabled = true
            binding.contractAddress.setTextColor(activity?.getColor(R.color.text_pink) ?: Color.RED)
            binding.contractAddress.setBackgroundResource(R.drawable.rounded_pink)
            binding.contractAddress.setOnFocusChangeListener { _, _ ->
                binding.contractAddress.setTextColor(activity?.getColor(R.color.text_blue) ?: Color.BLUE)
                binding.contractAddress.setBackgroundResource(R.drawable.rounded_light_grey)
                binding.error.visibility = View.GONE
            }
            binding.error.visibility = View.VISIBLE
        }
    }
    // https://www.digitalocean.com/community/tutorials/android-searchview-example-tutorial
    private fun addTokens() {

    }
}
