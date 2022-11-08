package com.concordium.wallet.ui.cis2

import android.content.Context
import androidx.fragment.app.Fragment
import com.concordium.wallet.ui.cis2.TokensViewModel.Companion.TOKEN_DATA
import com.concordium.wallet.util.getSerializableFromBundle

abstract class TokensBaseFragment : Fragment() {
    private lateinit var tokenData: TokenData

    override fun onAttach(context: Context) {
        super.onAttach(context)
        tokenData = requireArguments().getSerializableFromBundle(TOKEN_DATA, TokenData::class.java)
    }
}
