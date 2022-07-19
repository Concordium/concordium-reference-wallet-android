package com.concordium.wallet.ui.passphrase.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.databinding.FragmentPassPhraseHiddenBinding
import com.concordium.wallet.uicore.afterMeasured

class PassPhraseHiddenFragment : PassPhraseBaseFragment() {
    private var _binding: FragmentPassPhraseHiddenBinding? = null
    private val binding get() = _binding!!

    companion object {
        @JvmStatic
        fun newInstance(viewModel: PassPhraseViewModel) = PassPhraseHiddenFragment().apply {
            arguments = Bundle().apply {
                putSerializable(PassPhraseViewModel.PASS_PHRASE_DATA, viewModel)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPassPhraseHiddenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rlTapToReveal.afterMeasured {
            // in Figma the cross is 367 x 334 in size
            val layoutParams = binding.rlTapToReveal.layoutParams
            layoutParams.height = (binding.root.width.toFloat() * (367f / 334f)).toInt()
            binding.rlTapToReveal.layoutParams = layoutParams
        }
        binding.rlTapToReveal.setOnClickListener {
            viewModel.reveal.value = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
