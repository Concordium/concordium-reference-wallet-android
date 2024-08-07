package com.concordium.wallet.core.crypto

import com.concordium.wallet.App
import com.concordium.wallet.data.cryptolib.AccountKeysAndRandomnessInput
import com.concordium.wallet.data.cryptolib.AccountKeysAndRandomnessOutput
import com.concordium.wallet.data.cryptolib.CreateAccountTransactionInput
import com.concordium.wallet.data.cryptolib.CreateAccountTransactionOutput
import com.concordium.wallet.data.cryptolib.CreateCredentialInputV1
import com.concordium.wallet.data.cryptolib.CreateCredentialOutputV1
import com.concordium.wallet.data.cryptolib.CreateTransferInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.DecryptAmountInput
import com.concordium.wallet.data.cryptolib.GenerateRecoveryRequestInput
import com.concordium.wallet.data.cryptolib.IdRequestAndPrivateDataOutputV1
import com.concordium.wallet.data.cryptolib.IdentityKeysAndRandomnessInput
import com.concordium.wallet.data.cryptolib.IdentityKeysAndRandomnessOutput
import com.concordium.wallet.data.cryptolib.ParameterToJsonInput
import com.concordium.wallet.data.cryptolib.SerializeTokenTransferParametersInput
import com.concordium.wallet.data.cryptolib.SerializeTokenTransferParametersOutput
import com.concordium.wallet.data.cryptolib.SignMessageInput
import com.concordium.wallet.data.cryptolib.SignMessageOutput
import com.concordium.wallet.data.model.ArsInfo
import com.concordium.wallet.data.model.BakerKeys
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.IdentityProviderInfo
import com.concordium.wallet.util.AssetUtil
import com.google.gson.Gson

class CryptoLibraryMock(val gson: Gson) : CryptoLibrary {
    override suspend fun createIdRequestAndPrivateDataV1(
        identityProviderInfo: IdentityProviderInfo,
        arsInfo: Map<String, ArsInfo>,
        global: GlobalParams?,
        seed: String, net: String, identityIndex: Int
    ): IdRequestAndPrivateDataOutputV1? {
        val output = AssetUtil.loadFromAsset(
            App.appContext,
            "1.2.2.RX-lib_create_id_request_and_private_data.json"
        )
        return gson.fromJson(output, IdRequestAndPrivateDataOutputV1::class.java)
    }

    override suspend fun createCredentialV1(credentialInput: CreateCredentialInputV1): CreateCredentialOutputV1? {
        val output = AssetUtil.loadFromAsset(App.appContext, "2.2.2.RX-lib_create_credential.json")
        return gson.fromJson(output, CreateCredentialOutputV1::class.java)
    }

    override suspend fun createTransfer(
        createTransferInput: CreateTransferInput,
        type: Int
    ): CreateTransferOutput? {
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

    override suspend fun generateBakerKeys(): BakerKeys? {
        val output = AssetUtil.loadFromAsset(App.appContext, "5.3.2.RX_generate_baker_keys.json")
        return gson.fromJson(output, BakerKeys::class.java)
    }

    override suspend fun generateRecoveryRequest(recoveryRequestInput: GenerateRecoveryRequestInput): String? {
        TODO("Not yet implemented")
    }

    override suspend fun getIdentityKeysAndRandomness(identityKeysAndRandomnessInput: IdentityKeysAndRandomnessInput): IdentityKeysAndRandomnessOutput? {
        TODO("Not yet implemented")
    }

    override suspend fun getAccountKeysAndRandomness(accountKeysAndRandomnessInput: AccountKeysAndRandomnessInput): AccountKeysAndRandomnessOutput? {
        TODO("Not yet implemented")
    }

    override suspend fun createAccountTransaction(createAccountTransactionInput: CreateAccountTransactionInput): CreateAccountTransactionOutput? {
        TODO("Not yet implemented")
    }

    override suspend fun signMessage(signMessageInput: SignMessageInput): SignMessageOutput? {
        TODO("Not yet implemented")
    }

    override suspend fun serializeTokenTransferParameters(serializeTokenTransferParametersInput: SerializeTokenTransferParametersInput): SerializeTokenTransferParametersOutput? {
        TODO("Not yet implemented")
    }

    override suspend fun parameterToJson(parameterToJsonInput: ParameterToJsonInput): String? {
        TODO("Not yet implemented")
    }
}
