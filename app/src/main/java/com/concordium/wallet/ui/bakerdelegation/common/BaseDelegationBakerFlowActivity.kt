package com.concordium.wallet.ui.bakerdelegation.common

import android.os.Bundle
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.common.GenericFlowActivity

abstract class BaseDelegationBakerFlowActivity(titleId: Int) :
    GenericFlowActivity(titleId) {

    var bakerDelegationData: BakerDelegationData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.extras?.getSerializable(EXTRA_DELEGATION_BAKER_DATA)?.let {
            bakerDelegationData = it as BakerDelegationData
        }
    }

    abstract fun getTitles(): IntArray

    override fun getMaxPages(): Int {
        return getTitles().size
    }

    override fun getPageTitle(position: Int): Int {
        return getTitles()[position]
    }
}
