package com.concordium.wallet.util

import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import java.math.BigInteger


object TokenUtil {

    fun getCCDToken(account: Account?): Token {
        val totalUnshieldedBalance = account?.totalUnshieldedBalance
            ?: BigInteger.ZERO
        val atDisposal =
            account?.getAtDisposalWithoutStakedOrScheduled(totalUnshieldedBalance)
                ?: BigInteger.ZERO

        return Token(
            id = "",
            token = "CCD",
            totalSupply = "",
            tokenMetadata = null,
            isSelected = false,
            contractIndex = "",
            subIndex = "",
            isCCDToken = true,
            totalBalance = totalUnshieldedBalance,
            atDisposal = atDisposal,
            contractName = "",
            symbol = "CCD"
        )
    }
}
