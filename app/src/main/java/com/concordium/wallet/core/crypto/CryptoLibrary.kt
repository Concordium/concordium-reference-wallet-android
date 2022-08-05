package com.concordium.wallet.core.crypto

import com.concordium.wallet.data.cryptolib.*
import com.concordium.wallet.data.model.*

interface CryptoLibrary {
    companion object {
        const val SUCCESS: Int = 1
        const val REGULAR_TRANSFER: Int = 0
        const val PUBLIC_TO_SEC_TRANSFER: Int = 1
        const val SEC_TO_PUBLIC_TRANSFER: Int = 2
        const val ENCRYPTED_TRANSFER: Int = 3
        const val CONFIGURE_DELEGATION_TRANSACTION: Int = 4
        const val CONFIGURE_BAKING_TRANSACTION: Int = 5
    }

    suspend fun createIdRequestAndPrivateDataV1(identityProviderInfo: IdentityProviderInfo, arsInfo: Map<String, ArsInfo>, global: GlobalParams?, seed: String, net: String, identityIndex: Int): IdRequestAndPrivateDataOutputV1?

    suspend fun createCredentialV1(credentialInput: CreateCredentialInputV1): CreateCredentialOutputV1?

    suspend fun createTransfer(createTransferInput: CreateTransferInput, type: Int): CreateTransferOutput?

    fun checkAccountAddress(address: String): Boolean

    fun combineEncryptedAmounts(selfAmount: String, incomingAmounts: String): String?

    suspend fun decryptEncryptedAmount(input: DecryptAmountInput): String?

    suspend fun generateBakerKeys(): BakerKeys?
}