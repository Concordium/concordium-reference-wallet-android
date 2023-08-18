package com.concordium.wallet.util

import java.math.BigInteger
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account


object TokenUtil {

    fun getCCDToken(account: Account?): Token {
        val atDisposal = account?.getAtDisposalWithoutStakedOrScheduled(account.totalBalance) ?: BigInteger.ZERO
        return Token("", "CCD", "", null, false, "", "",true, (account?.totalBalance ?: BigInteger.ZERO), atDisposal, "", "CCD")
    }
}
