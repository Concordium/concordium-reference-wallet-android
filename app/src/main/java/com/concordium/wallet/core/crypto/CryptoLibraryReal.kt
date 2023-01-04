package com.concordium.wallet.core.crypto

import com.concordium.mobile_wallet_lib.*
import com.concordium.wallet.data.cryptolib.*
import com.concordium.wallet.data.model.*
import com.concordium.wallet.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CryptoLibraryReal(val gson: Gson) : CryptoLibrary {
    override suspend fun createIdRequestAndPrivateDataV1(
        identityProviderInfo: IdentityProviderInfo,
        arsInfo: Map<String, ArsInfo>,
        global: GlobalParams?,
        seed: String, net: String, identityIndex: Int
    ): IdRequestAndPrivateDataOutputV1? =
        withContext(Dispatchers.Default) {
            val inputObj = IdRequestAndPrivateDataInputV1(
                identityProviderInfo,
                global,
                arsInfo,
                seed,
                net,
                identityIndex
            )
            val input = gson.toJson(inputObj)
            loadWalletLib()
            val result = create_id_request_and_private_data_v1(input)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext gson.fromJson(
                    result.output,
                    IdRequestAndPrivateDataOutputV1::class.java
                )
            }
            Log.e("CryptoLib failed")
            return@withContext null
        }

    override suspend fun createCredentialV1(credentialInput: CreateCredentialInputV1): CreateCredentialOutputV1? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(credentialInput)
            loadWalletLib()
            val result = create_credential_v1(input)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext gson.fromJson(
                    result.output,
                    CreateCredentialOutputV1::class.java
                )
            }
            Log.e("CryptoLib failed")
            return@withContext null
        }

    override suspend fun createTransfer(
        createTransferInput: CreateTransferInput,
        type: Int
    ): CreateTransferOutput? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(createTransferInput)
            Log.d("Input: $input")
            loadWalletLib()
            val result = internalCreateTransfer(input, type)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext gson.fromJson(result.output, CreateTransferOutput::class.java)
            }
            Log.e("CryptoLib failed")
            return@withContext null
        }

    override suspend fun proveIdStatement(statement: ProofsInput): String? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(statement)
            Log.d("Input: $input")
            loadWalletLib()
            val result = prove_id_statement(input)
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext result.output
            }
            Log.e("CryptoLib failed : Result = ${result.result}, Output = ${result.output} ")
            return@withContext null
        }

    private fun internalCreateTransfer(input: String, type: Int): ReturnValue {
        when (type) {
            CryptoLibrary.PUBLIC_TO_SEC_TRANSFER -> return create_pub_to_sec_transfer(input)
            CryptoLibrary.SEC_TO_PUBLIC_TRANSFER -> return create_sec_to_pub_transfer(input)
            CryptoLibrary.ENCRYPTED_TRANSFER -> return create_encrypted_transfer(input)
            CryptoLibrary.CONFIGURE_DELEGATION_TRANSACTION -> return create_configure_delegation_transaction(
                input
            )
            CryptoLibrary.CONFIGURE_BAKING_TRANSACTION -> return create_configure_baker_transaction(
                input
            )
        }
        return create_transfer(input)
    }

    override suspend fun decryptEncryptedAmount(input: DecryptAmountInput): String? =
        withContext(Dispatchers.Default) {
            val encryptedInput = gson.toJson(input)
            Log.d("Input: $encryptedInput")
            loadWalletLib()
            val result = decrypt_encrypted_amount(encryptedInput)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext result.output
            }
            Log.e("CryptoLib failed")
            return@withContext null
        }

    override fun combineEncryptedAmounts(selfAmount: String, incomingAmounts: String): String {
        loadWalletLib()
        val result =
            combine_encrypted_amounts(gson.toJson(selfAmount), gson.toJson(incomingAmounts))
        return gson.fromJson(result.output, String::class.java)
    }

    override fun checkAccountAddress(address: String): Boolean {
        loadWalletLib()
        return check_account_address(address)
    }

    override suspend fun generateBakerKeys(): BakerKeys? =
        withContext(Dispatchers.Default) {
            loadWalletLib()
            val result = generate_baker_keys()
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext gson.fromJson(result.output, BakerKeys::class.java)
            }
            Log.e("CryptoLib failed")
            return@withContext null
        }

    override suspend fun generateRecoveryRequest(recoveryRequestInput: GenerateRecoveryRequestInput): String? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(recoveryRequestInput)
            loadWalletLib()
            val result = generate_recovery_request(input)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext result.output
            }
            Log.e("CryptoLib failed")
            return@withContext null
        }

    override suspend fun getIdentityKeysAndRandomness(identityKeysAndRandomnessInput: IdentityKeysAndRandomnessInput): IdentityKeysAndRandomnessOutput? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(identityKeysAndRandomnessInput)
            loadWalletLib()
            val result = get_identity_keys_and_randomness(input)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext gson.fromJson(
                    result.output,
                    IdentityKeysAndRandomnessOutput::class.java
                )
            }
            Log.e("CryptoLib failed")
            return@withContext null
        }

    override suspend fun getAccountKeysAndRandomness(accountKeysAndRandomnessInput: AccountKeysAndRandomnessInput): AccountKeysAndRandomnessOutput? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(accountKeysAndRandomnessInput)
            loadWalletLib()
            val result = get_account_keys_and_randomness(input)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext gson.fromJson(
                    result.output,
                    AccountKeysAndRandomnessOutput::class.java
                )
            }
            Log.e("CryptoLib failed")
            return@withContext null
        }

    override suspend fun createAccountTransaction(createAccountTransactionInput: CreateAccountTransactionInput): CreateAccountTransactionOutput? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(createAccountTransactionInput)
            loadWalletLib()
            val result = create_account_transaction(input)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext gson.fromJson(
                    result.output,
                    CreateAccountTransactionOutput::class.java
                )
            }
            Log.e("CryptoLib failed")
            return@withContext null
        }

    override suspend fun signMessage(signMessageInput: SignMessageInput): SignMessageOutput? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(signMessageInput)
            loadWalletLib()
            val result = sign_message(input)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext gson.fromJson(result.output, SignMessageOutput::class.java)
            }
            Log.e("CryptoLib failed")
            return@withContext null
        }

    override suspend fun serializeTokenTransferParameters(serializeTokenTransferParametersInput: SerializeTokenTransferParametersInput): SerializeTokenTransferParametersOutput? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(serializeTokenTransferParametersInput)
            loadWalletLib()
            val result = serialize_token_transfer_parameters(input)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext gson.fromJson(
                    result.output,
                    SerializeTokenTransferParametersOutput::class.java
                )
            }
            Log.e("CryptoLib failed")
            return@withContext null
        }

    override suspend fun parameterToJson(parameterToJsonInput: ParameterToJsonInput): String? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(parameterToJsonInput)
            loadWalletLib()
            val result = parameter_to_json(input)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext result.output
            }
            Log.e("CryptoLib failed")
            return@withContext null
        }
}
