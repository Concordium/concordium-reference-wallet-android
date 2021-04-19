package com.concordium.wallet.core.crypto

import com.concordium.wallet.App
import com.concordium.wallet.data.cryptolib.*
import com.concordium.wallet.data.model.ArsInfo
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.IdentityProviderInfo
import com.concordium.wallet.data.model.PossibleAccount
import com.concordium.wallet.util.AssetUtil
import com.google.gson.Gson

class CryptoLibraryMock(val gson: Gson) : CryptoLibrary {

    override suspend fun createIdRequestAndPrivateData(
        identityProviderInfo: IdentityProviderInfo,
        arsInfo: Map<String, ArsInfo>,
        global: GlobalParams?
    ): IdRequestAndPrivateDataOutput? {
        val output = AssetUtil.loadFromAsset(
            App.appContext,
            "1.2.2.RX-lib_create_id_request_and_private_data.json"
        )
        return gson.fromJson(output, IdRequestAndPrivateDataOutput::class.java)
    }

    override suspend fun createCredential(credentialInput: CreateCredentialInput): CreateCredentialOutput? {
        val output = AssetUtil.loadFromAsset(App.appContext, "2.2.2.RX-lib_create_credential.json")
        return gson.fromJson(output, CreateCredentialOutput::class.java)
    }

    override suspend fun createTransfer(createTransferInput: CreateTransferInput, type: Int): CreateTransferOutput? {
        val output = AssetUtil.loadFromAsset(App.appContext, "3.2.2.RX-lib_create_transfer.json")
        return gson.fromJson(output, CreateTransferOutput::class.java)
    }

    override fun checkAccountAddress(address: String): Boolean {
        return true
    }

    override fun combineEncryptedAmounts(selfAmount: String, incomingAmounts: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun decryptEncryptedAmount(input: DecryptAmountInput): String? {
        TODO("Not yet implemented")
    }

    override suspend fun generateAccounts(generateAccountsInput: GenerateAccountsInput): List<PossibleAccount>? {
        return null
    }
}