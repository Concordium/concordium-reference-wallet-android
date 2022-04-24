package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.Account
import java.io.Serializable

/**
 * Class used for collecting data from AccountDetails all the way to submission
 */
data class DelegationData(
    var account: Account? = null,
    var restake: Boolean = true,
    var isLPool: Boolean = false,
    var isBakerPool: Boolean = true,
    var poolId: String = "",
    var isOpenBaker: Boolean = true,
    var isClosedBaker: Boolean = false,
    var isTransactionInProgress: Boolean = false,
    var type: String
    ) : Serializable {

    companion object {
        const val TYPE_REGISTER_DELEGATION = "TYPE_REGISTER_DELEGATION"
        const val TYPE_UPDATE_DELEGATION = "TYPE_UPDATE_DELEGATION"
        const val TYPE_REMOVE_DELEGATION = "TYPE_REMOVE_DELEGATION"

        const val TYPE_REGISTER_BAKER = "TYPE_REGISTER_BAKER"
        const val TYPE_UPDATE_BAKER = "TYPE_UPDATE_BAKER"
        const val TYPE_REMOVE_BAKER = "TYPE_REMOVE_BAKER"
    }

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
}
