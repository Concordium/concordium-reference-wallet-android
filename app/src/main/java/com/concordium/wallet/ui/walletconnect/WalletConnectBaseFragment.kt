package com.concordium.wallet.ui.walletconnect

import android.content.Context
import androidx.fragment.app.Fragment
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA

abstract class WalletConnectBaseFragment : Fragment() {
    private lateinit var walletConnectData: WalletConnectData

    override fun onAttach(context: Context) {
        super.onAttach(context)
        arguments?.getSerializable(WALLET_CONNECT_DATA)?.let {
            walletConnectData = it as WalletConnectData
        }
    }
}
