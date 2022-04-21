package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.Account
import java.io.Serializable

/**
 * Class used for collecting data from AccountDetails all the way to submission
 */
data class BakerData(
    var account: Account? = null,
    var isOpenBaker: Boolean = true,
    var isClosedBaker: Boolean = false
//    var type: String,
    ) : Serializable {

    companion object {
        const val TYPE_REGISTER_DELEGATION = "TYPE_REGISTER_DELEGATION"
        const val TYPE_UPDATE_DELEGATION = "TYPE_UPDATE_DELEGATION"
        const val TYPE_REMOVE_DELEGATION = "TYPE_REMOVE_DELEGATION"
    }
/*
    var transferSubmissionStatus: TransferSubmissionStatus? = null
    var submissionId: String? = null
    var energy: Long? = null
    var accountNonce: AccountNonce? = null
    var amount: Long? = null
    var chainParameters: ChainParameters? = null
    var bakerPoolStatus: BakerPoolStatus? = null
    var cost: Long? = null

    var oldStakedAmount: Long? = null
    var oldRestake: Boolean? = null
    var oldDelegationIsBaker: Boolean? = null
    var oldDelegationTargetPoolId: Long? = null
*/
}
