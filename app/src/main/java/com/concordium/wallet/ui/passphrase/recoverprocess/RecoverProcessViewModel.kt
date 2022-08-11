package com.concordium.wallet.ui.passphrase.recoverprocess

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.cryptolib.CreateCredentialInputV1
import com.concordium.wallet.data.cryptolib.GenerateRecoveryRequestInput
import com.concordium.wallet.data.cryptolib.GenerateRecoveryRequestOutput
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.IdentityWithAccounts
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Serializable

class RecoverProcessViewModel(application: Application) : AndroidViewModel(application), Serializable {
    companion object {
        const val RECOVER_PROCESS_DATA = "RECOVER_PROCESS_DATA"
        const val STATUS_OK = 1
        const val STATUS_NOTHING_TO_RECOVER = 2
    }

    var identitiesWithAccounts: List<IdentityWithAccounts> = mutableListOf()
    val statusChanged: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    fun startScanning() {

        val net = "Mainnet"
        val identityIndex = 1
        val seed = AuthPreferences(getApplication()).getSeedPhrase()

        viewModelScope.launch {
            waiting.value = true

            val repository = IdentityProviderRepository()
            val globalInfo = repository.getGlobalInfoSuspended()
            val identityProviders = repository.getIdentityProviderInfoSuspended()

            identityProviders.forEach { identityProvider ->
                val recoveryRequestInput = GenerateRecoveryRequestInput(
                    identityProvider.ipInfo,
                    globalInfo.value,
                    seed,
                    net,
                    identityIndex,
                    System.currentTimeMillis() / 1000
                )

                val output = App.appCore.cryptoLibrary.generateRecoveryRequest(recoveryRequestInput)

                println(output)
            }

            waiting.value = false
            statusChanged.value = 1
        }

    }
/*
    fun startScanning() {
        val passPhrase = AuthPreferences(getApplication()).getSeedPhrase()

        viewModelScope.launch {
            val data = mutableListOf<IdentityWithAccounts>()
            data.add(IdentityWithAccounts(
                identity = dummyIdentity(1),
                accounts = dummyAccounts(1,2)
            ))
            data.add(IdentityWithAccounts(
                identity = dummyIdentity(2),
                accounts = dummyAccounts(2,10)
            ))
            data.add(IdentityWithAccounts(
                identity = dummyIdentity(3),
                accounts = dummyAccounts(3,1)
            ))
            data.add(IdentityWithAccounts(
                identity = dummyIdentity(4),
                accounts = dummyAccounts(5,8)
            ))

            delay(3000)
            identitiesWithAccounts = data
            statusChanged.value = 1
        }
    }

    private fun dummyAccounts(identityId: Int, count: Int): List<Account> {
        val list = mutableListOf<Account>()
        for (i in 1 until count + 1) {
            list.add(dummyAccount(identityId, i))
        }
        return list
    }

    private fun dummyAccount(identityId: Int, id: Int): Account {
        val revealedAttributes = ArrayList<IdentityAttribute>().apply {
            add(IdentityAttribute("name1", "value1"))
            add(IdentityAttribute("name2", "value2"))
        }
        return Account(
            id,
            identityId,
            "Account $id",
            "0",
            "0",
            TransactionStatus.UNKNOWN,
            "",
            revealedAttributes,
            CredentialWrapper(RawJson("{}"), 1),
            (id * 5000).toLong(),
            (id * 5000).toLong(),
            (id * 5000).toLong(),
            0,
            0,
            null,
            null,
            ShieldedAccountEncryptionStatus.ENCRYPTED,
            0,
            0,
            false,
            null,
            null,
            null,
            null)
    }

    private fun dummyIdentity(id: Int): Identity {
        val identityProviderInfo = IdentityProviderInfo(
            0,
            IdentityProviderDescription("description", "ID Provider", "url"),
            "",
            ""
        )
        val arsInfos = HashMap<String, ArsInfo>()
        arsInfos.put("1", ArsInfo(1, "", ArDescription("","", "")))
        val identityProvider =
            IdentityProvider(identityProviderInfo, arsInfos, IdentityProviderMetaData("", "", ""))
        val pubInfoForIP = PubInfoForIp("", RawJson("{}"), "")
        val preIdentityObject =
            PreIdentityObject(
                RawJson("{}"), pubInfoForIP, "",
                RawJson("{}"), "",
                RawJson("{}"), "", ""
            )
        val identityObject =
            IdentityObject(
                AttributeList(HashMap(), "202003", 345, "201903"),
                preIdentityObject,
                RawJson("{}")
            )
        val identity = Identity(id, "Identity $id", "", "","", 0, identityProvider, identityObject, "")
        return identity
    }
*/
}
