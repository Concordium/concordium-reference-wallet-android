package com.concordium.wallet.ui.bakerdelegation.delegation

import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import kotlinx.android.synthetic.main.activity_delegation_remove.*
import javax.crypto.Cipher

abstract class BaseDelegationActivity(layout: Int, titleId: Int = R.string.app_name) :
    BaseDelegationBakerActivity(layout, titleId) {

}
