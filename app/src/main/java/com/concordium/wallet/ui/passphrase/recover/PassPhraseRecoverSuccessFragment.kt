package com.concordium.wallet.ui.passphrase.recover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentPassPhraseRecoverSuccessBinding
import com.concordium.wallet.ui.base.BaseActivity

class PassPhraseRecoverSuccessFragment : Fragment() {
    private var _binding: FragmentPassPhraseRecoverSuccessBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPassPhraseRecoverSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as BaseActivity).setActionBarTitle(R.string.pass_phrase_recover_input_title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
