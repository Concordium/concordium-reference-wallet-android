package com.concordium.wallet.core.crypto

import com.concordium.mobile_wallet_lib.*
import com.concordium.wallet.data.cryptolib.*
import com.concordium.wallet.data.model.*
import com.concordium.wallet.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CryptoLibraryReal(val gson: Gson) : CryptoLibrary {

    override suspend fun createIdRequestAndPrivateData(
        identityProviderInfo: IdentityProviderInfo,
        arsInfo: Map<String, ArsInfo>,
        global: GlobalParams?
    ): IdRequestAndPrivateDataOutput? =
        withContext(Dispatchers.Default) {
            val inputObj = IdRequestAndPrivateDataInput(identityProviderInfo, global, arsInfo)
            val input = gson.toJson(inputObj)
            loadWalletLib()
            val result = create_id_request_and_private_data(input)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                val output = gson.fromJson(result.output, IdRequestAndPrivateDataOutput::class.java)
                return@withContext output
            }
            Log.e("Cryptolib failed")
            return@withContext null
        }

    override suspend fun createCredential(credentialInput: CreateCredentialInput): CreateCredentialOutput? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(credentialInput)
            loadWalletLib()
            val result = create_credential(input)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                val output = gson.fromJson(result.output, CreateCredentialOutput::class.java)
                return@withContext output
            }
            Log.e("Cryptolib failed")
            return@withContext null
        }


    override suspend fun createTransfer(createTransferInput: CreateTransferInput, type: Int): CreateTransferOutput? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(createTransferInput)
            Log.d("Input: $input")
            loadWalletLib()
            val result = internalCreateTransfer(input, type)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                val output = gson.fromJson(result.output, CreateTransferOutput::class.java)
                return@withContext output
            }
            Log.e("Cryptolib failed")
            return@withContext null
        }

    private fun internalCreateTransfer(input: String, type: Int): ReturnValue {
        when (type) {
            CryptoLibrary.PUBLIC_TO_SEC_TRANSFER -> return create_pub_to_sec_transfer(input)
            CryptoLibrary.SEC_TO_PUBLIC_TRANSFER -> return create_sec_to_pub_transfer(input)
            CryptoLibrary.ENCRYPTED_TRANSFER -> return create_encrypted_transfer(input)
            CryptoLibrary.CONFIGURE_DELEGATION_TRANSACTION -> return create_configure_delegation_transaction(input)
            CryptoLibrary.CONFIGURE_BAKING_TRANSACTION -> return create_configure_baker_transaction(input)
        }
        return create_transfer(input) // CryptoLibrary.REGULAR_TRANSFER
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
            Log.e("Cryptolib failed")
            return@withContext null
        }

    override fun combineEncryptedAmounts(selfAmount: String, incomingAmounts: String): String {
        loadWalletLib()
        val result = combine_encrypted_amounts(gson.toJson(selfAmount), gson.toJson(incomingAmounts))
        return gson.fromJson(result.output, String::class.java)
    }

    override fun checkAccountAddress(address: String): Boolean {
        loadWalletLib()
        return check_account_address(address)
    }

    override suspend fun generateAccounts(generateAccountsInput: GenerateAccountsInput): List<PossibleAccount>? =
        withContext(Dispatchers.Default) {
            val input = gson.toJson(generateAccountsInput)
            loadWalletLib()
            val result = generate_accounts(input)
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                val listType = object : TypeToken<List<PossibleAccount>>() {}.type
                return@withContext gson.fromJson<List<PossibleAccount>>(result.output, listType)
            }
            Log.e("Cryptolib failed")
            return@withContext null
        }

    override suspend fun generateBakerKeys(): BakerKeys? =
        withContext(Dispatchers.Default) {
            loadWalletLib()
            val result = generate_baker_keys()
            Log.d("Output (Code ${result.result}): ${result.output}")
            if (result.result == CryptoLibrary.SUCCESS) {
                return@withContext gson.fromJson(result.output, BakerKeys::class.java)
            }
            Log.e("Cryptolib failed")
            return@withContext null
        }
}