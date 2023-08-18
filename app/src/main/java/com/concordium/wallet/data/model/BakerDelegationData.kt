package com.concordium.wallet.data.model

import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.CONFIGURE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REMOVE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_KEYS
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_STAKE
import com.concordium.wallet.data.room.Account
import java.io.Serializable
import java.math.BigInteger

/**
 * Class used for collecting data from AccountDetails all the way to submission
 */
data class BakerDelegationData(
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

    var transferSubmissionStatus: TransferSubmissionStatus? = null
    var submissionId: String? = null
    var energy: Long? = null
    var accountNonce: AccountNonce? = null
    var amount: BigInteger? = null
    var chainParameters: ChainParameters? = null
    var bakerPoolStatus: BakerPoolStatus? = null
    var passiveDelegation: PassiveDelegation? = null
    var cost: BigInteger? = null
    var metadataUrl: String? = null

    fun isUpdateBaker(): Boolean {
        return type == UPDATE_BAKER_STAKE || type == UPDATE_BAKER_POOL || type == UPDATE_BAKER_KEYS || type == CONFIGURE_BAKER
    }

    var oldStakedAmount: BigInteger? = null
    var oldRestake: Boolean? = null
    var oldDelegationIsBaker: Boolean? = null
    var oldDelegationTargetPoolId: Long? = null
    var oldMetadataUrl: String? = null
    var oldOpenStatus: String? = null

    fun isBakerFlow(): Boolean {
        return type == REGISTER_BAKER || type == UPDATE_BAKER_STAKE || type == UPDATE_BAKER_POOL || type == UPDATE_BAKER_KEYS || type == REMOVE_BAKER || type == CONFIGURE_BAKER
    }
}
