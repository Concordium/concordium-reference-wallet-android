package com.concordium.wallet.ui.walletconnect.proof.of.identity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.concordium.wallet.databinding.FragmentProofOfIdentityBinding
import com.concordium.wallet.ui.base.BaseBottomSheetDialogFragment
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel

class ProofOfIdentityFragment : BaseBottomSheetDialogFragment() {
    private var _binding: FragmentProofOfIdentityBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WalletConnectViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProofOfIdentityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
    }

    private fun initViews() {
        binding.viewPager.adapter = ProofOfIdentityAdapter(requireActivity())
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.currentItem = 0
    }

    private fun initObservers() {
        viewModel.stepPageBy.observe(this) {
            if (binding.viewPager.currentItem + it >= 0 && binding.viewPager.currentItem + it < (binding.viewPager.adapter?.itemCount ?: 0)) {
                binding.viewPager.currentItem = binding.viewPager.currentItem + it
            }

        }
    }

    override fun onResume() {
        super.onResume()
        binding.viewPager.currentItem = 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class ProofOfIdentityAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ProofOfIdentityPromptFragment()
                1 -> ProofOfIdentityStatusFragment()
                else -> Fragment()
            }
        }
    }
}