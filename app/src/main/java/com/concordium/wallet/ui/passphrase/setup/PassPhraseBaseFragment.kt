package com.concordium.wallet.ui.passphrase.setup

import android.content.Context
import androidx.fragment.app.Fragment
import com.concordium.wallet.ui.passphrase.setup.PassPhraseViewModel.Companion.PASS_PHRASE_DATA

abstract class PassPhraseBaseFragment : Fragment() {
    protected lateinit var viewModel: PassPhraseViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        arguments?.getSerializable(PASS_PHRASE_DATA)?.let {
            viewModel = it as PassPhraseViewModel
        }
    }
}
