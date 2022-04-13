package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.Account
import java.io.Serializable

/**
 * Class used for collecting data from AccountDetails all the way to submission
 */
data class DelegationData(
    var account: Account? = null,
    var restake: Boolean = false,
    var isLPool: Boolean = false,
    var isBakerPool: Boolean = true,
    var poolId: String = "",
    var type: String,
    var oldPoolId: String = ""
    ) : Serializable {

    companion object {
        const val TYPE_REGISTER_DELEGATION = "TYPE_REGISTER_DELEGATION"
        const val TYPE_UPDATE_DELEGATION = "TYPE_UPDATE_DELEGATION"
        const val TYPE_REMOVE_DELEGATION = "TYPE_REMOVE_DELEGATION"
    }

    var energy: Long? = null
    var accountNonce: AccountNonce? = null
    var amount: Long? = null
    var chainParameters: ChainParameters? = null
    var bakerPoolStatus: BakerPoolStatus? = null

}