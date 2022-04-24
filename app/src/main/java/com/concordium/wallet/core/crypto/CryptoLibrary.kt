package com.concordium.wallet.core.crypto

import com.concordium.wallet.data.cryptolib.*
import com.concordium.wallet.data.model.*

interface CryptoLibrary {

    companion object {
        val SUCCESS: Int = 1
        val FAILURE: Int = 0
        val REGULAR_TRANSFER: Int = 0
        val PUBLIC_TO_SEC_TRANSFER: Int = 1
        val SEC_TO_PUBLIC_TRANSFER: Int = 2
        val ENCRYPTED_TRANSFER: Int = 3
        val CONFIGURE_DELEGATION_TRANSACTION: Int = 4
    }

    suspend fun createIdRequestAndPrivateData(
        identityProviderInfo: IdentityProviderInfo,
        arsInfo: Map<String, ArsInfo>,
        global: GlobalParams?
    ): IdRequestAndPrivateDataOutput?

    suspend fun createCredential(credentialInput: CreateCredentialInput): CreateCredentialOutput?

    suspend fun createTransfer(createTransferInput: CreateTransferInput, type: Int): CreateTransferOutput?

    fun checkAccountAddress(address: String): Boolean

    fun combineEncryptedAmounts(selfAmount: String, incomingAmounts: String): String?

    suspend fun decryptEncryptedAmount(input: DecryptAmountInput): String?

    suspend fun generateAccounts(generateAccountsInput: GenerateAccountsInput): List<PossibleAccount>?

    suspend fun generateBakerKeys(): BakerKeys?
}