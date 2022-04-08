package com.concordium.wallet.ui.bakerdelegation.common

import android.os.Bundle
import com.concordium.wallet.data.model.DelegationData
import com.concordium.wallet.ui.common.GenericFlowActivity

abstract class BaseDelegationBakerFlowActivity(titleId: Int) :
    GenericFlowActivity(titleId) {

    var delegationData: DelegationData? = null

    companion object {
        const val EXTRA_DELEGATION_DATA = "EXTRA_DELEGATION_DATA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegationData = intent.extras?.getSerializable(EXTRA_DELEGATION_DATA) as DelegationData
    }

    abstract fun getTitles(): IntArray

    override fun getMaxPages(): Int {
        return getTitles().size
    }

    override fun getPageTitle(position: Int): Int {
        return getTitles()[position]
    }

}
