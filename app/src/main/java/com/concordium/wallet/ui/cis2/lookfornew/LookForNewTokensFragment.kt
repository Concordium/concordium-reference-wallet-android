package com.concordium.wallet.ui.cis2.lookfornew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.concordium.wallet.databinding.FragmentDialogLookForNewTokensBinding
import com.concordium.wallet.ui.base.BaseBottomSheetDialogFragment
import com.concordium.wallet.ui.cis2.TokensViewModel

class LookForNewTokensFragment : BaseBottomSheetDialogFragment() {
    private var _binding: FragmentDialogLookForNewTokensBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TokensViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDialogLookForNewTokensBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        binding.viewPager.currentItem = 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.viewPager.adapter = LookForNewTokensAdapter(requireActivity())
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.currentItem = 0
    }

    private fun initObservers() {
        viewModel.waitingNewTokens.observe(this) { waiting ->
            if (!waiting && viewModel.tokens.isNotEmpty())
                binding.viewPager.currentItem++
        }
        viewModel.stepPageBy.observe(this) {
            if (binding.viewPager.currentItem + it >= 0 && binding.viewPager.currentItem + it < (binding.viewPager.adapter?.itemCount ?: 0)) {
                binding.viewPager.currentItem = binding.viewPager.currentItem + it
            }
        }
    }

    private inner class LookForNewTokensAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ContractAddressFragment()
                1 -> SelectTokensFragment()
                2 -> TokenDetailsFragment()
                else -> Fragment()
            }
        }
    }
}
