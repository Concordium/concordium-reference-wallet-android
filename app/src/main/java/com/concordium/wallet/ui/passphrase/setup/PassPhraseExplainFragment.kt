package com.concordium.wallet.ui.passphrase.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.concordium.wallet.databinding.FragmentPassPhraseExplainedBinding
import com.concordium.wallet.uicore.afterMeasured

class PassPhraseExplainFragment : Fragment() {
    private var _binding: FragmentPassPhraseExplainedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPassPhraseExplainedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.crossHideView.afterMeasured {
            // in Figma the view is 203 x 342.74 in size
            val layoutParams = binding.crossHideView.layoutParams
            layoutParams.height = (binding.root.width.toFloat() * (203f / 342.74f)).toInt()
            binding.crossHideView.layoutParams = layoutParams
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}