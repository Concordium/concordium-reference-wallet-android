package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.Account
import java.io.Serializable

/**
 * Class used for collecting data from AccountDetails all the way to submission
 */
data class BakerData2(
    var account: Account? = null,
    var restake: Boolean = true,
    var isOpenBaker: Boolean = true,
    var isClosedBaker: Boolean = false,
    var type: String
    ) : Serializable {

    companion object {
        const val TYPE_REGISTER_BAKER = "TYPE_REGISTER_BAKER"
        const val TYPE_UPDATE_BAKER = "TYPE_UPDATE_BAKER"
        const val TYPE_REMOVE_BAKER = "TYPE_REMOVE_BAKER"
    }

    var amount: Long? = null
    var energy: Long? = null
    var cost: Long? = null
}
