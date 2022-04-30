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
    var bakerPoolInfo: BakerPoolInfo? = BakerPoolInfo(BakerPoolInfo.OPEN_STATUS_OPEN_FOR_ALL),
    var isTransactionInProgress: Boolean = false,
    var bakerKeys: BakerKeys? = null,
    var type: String
    ) : Serializable {

    companion object {
        const val TYPE_REGISTER_DELEGATION = "TYPE_REGISTER_DELEGATION"
        const val TYPE_UPDATE_DELEGATION = "TYPE_UPDATE_DELEGATION"
        const val TYPE_REMOVE_DELEGATION = "TYPE_REMOVE_DELEGATION"
        const val TYPE_REGISTER_BAKER = "TYPE_REGISTER_BAKER"
        const val TYPE_UPDATE_BAKER_STAKE = "TYPE_UPDATE_BAKER_STAKE"
        const val TYPE_UPDATE_BAKER_POOL = "TYPE_UPDATE_BAKER_POOL"
        const val TYPE_UPDATE_BAKER_KEYS = "TYPE_UPDATE_BAKER_KEYS"
        const val TYPE_REMOVE_BAKER = "TYPE_REMOVE_BAKER"
        const val TYPE_CONFIGURE_BAKER = "TYPE_CONFIGURE_BAKER"
    }

    var transferSubmissionStatus: TransferSubmissionStatus? = null
    var submissionId: String? = null
    var energy: Long? = null
    var accountNonce: AccountNonce? = null
    var amount: Long? = null
    var chainParameters: ChainParameters? = null
    var bakerPoolStatus: BakerPoolStatus? = null
    var cost: Long? = null
    var metadataUrl: String? = null

    var oldStakedAmount: Long? = null
    var oldRestake: Boolean? = null
    var oldDelegationIsBaker: Boolean? = null
    var oldDelegationTargetPoolId: Long? = null
    var oldMetadataUrl: String? = null

    fun isBakerFlow(): Boolean {
        return bakerKeys != null
    }
}
