package com.concordium.wallet.ui.cis2

import com.concordium.wallet.data.cryptolib.CreateTransferInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TransferSubmissionStatus
import com.concordium.wallet.data.room.Account
import java.io.Serializable
import java.math.BigInteger

@Suppress("SerialVersionUIDInSerializableClass")
data class SendTokenData(
    var token: Token? = null,
    var account: Account? = null,
    var amount: BigInteger = BigInteger.ZERO,
    var receiver: String = "",
    var receiverName: String? = null,
    var fee: BigInteger? = null,
    var max: BigInteger? = null,
    var memo: String? = null,
    var energy: Long? = null,
    var maxContractExecutionEnergy: Long? = null,
    var accountNonce: AccountNonce? = null,
    var expiry: Long? = null,
    var createTransferInput: CreateTransferInput? = null,
    var createTransferOutput: CreateTransferOutput? = null,
    var receiverPublicKey: String? = null,
    var globalParams: GlobalParams? = null,
    var accountBalance: AccountBalance? = null,
    var newSelfEncryptedAmount: String? = null,
    var submissionId: String? = null,
    var transferSubmissionStatus: TransferSubmissionStatus? = null
) : Serializable