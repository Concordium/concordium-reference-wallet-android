package com.concordium.wallet.ui.cis2.lookfornew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.concordium.wallet.databinding.FragmentDialogTokenDetailsBinding
import com.concordium.wallet.ui.cis2.TokensViewModel

class TokenDetailsFragment : Fragment() {
    private var _binding: FragmentDialogTokenDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TokensViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDialogTokenDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.backToList.setOnClickListener {
            viewModel.stepPage(-1)
        }
    }
}
