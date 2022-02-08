package com.concordium.wallet.ui.more.dev

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.OfflineMockInterceptor
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.*
import kotlinx.coroutines.launch

class DevViewModel(application: Application) : AndroidViewModel(application) {

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository
    private val transferRepository: TransferRepository
    private val recipientRepository: RecipientRepository

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
        val transferDao = WalletDatabase.getDatabase(application).transferDao()
        transferRepository = TransferRepository(transferDao)
        val recipientDao = WalletDatabase.getDatabase(application).recipientDao()
        recipientRepository = RecipientRepository(recipientDao)
    }

    fun initialize() {

    }

    fun createData() = viewModelScope.launch {
        createIdentity()
        createAccounts()
        createTransfers()
        createRecipients()
    }

    private suspend fun createIdentity() {
        val identityProviderInfo = IdentityProviderInfo(
            0,
            IdentityProviderDescription("description", "ID Provider", "url"),
            "",
            ""
        )
        val arsInfos = HashMap<String, ArsInfo>();
        arsInfos.put("1",ArsInfo(1, "", ArDescription("","", "")))
        val identityProvider =
            IdentityProvider(identityProviderInfo, arsInfos, IdentityProviderMetaData("", "", ""))
        val pubInfoForIP = PubInfoForIp("", RawJson("{}"), "")
        val preIdentityObject =
            PreIdentityObject(
                RawJson("{}"), pubInfoForIP, "",
                RawJson("{}"), "",
                RawJson("{}"), ""
            )
        val identityObject =
            IdentityObject(
                AttributeList(HashMap(), "202003", 345, "201903"),
                preIdentityObject,
                RawJson("{}")
            )
        val identity = Identity(0, "identity name", "", "","", 0, identityProvider, identityObject, "")
        identityRepository.insert(identity)
    }

    private suspend fun createAccounts() {
        val identityList = identityRepository.getAll()
        val identityId = identityList.firstOrNull()?.id ?: 0

        val accountList = ArrayList<Account>()
        val revealedAttributes = ArrayList<IdentityAttribute>().apply {
            add(IdentityAttribute("name1", "value1"))
            add(IdentityAttribute("name2", "value2"))
            add(IdentityAttribute("name3", "value3"))
        }
        val credential = RawJson("{}")
        accountList.add(
            Account(
                0,
                identityId,
                "Finalized account",
                "address01",
                "a01",
                TransactionStatus.FINALIZED,
                "",
                revealedAttributes,
                CredentialWrapper(credential,0),
                0,
                0,
                0,
                0,
                0,
                null,
                null,
                ShieldedAccountEncryptionStatus.ENCRYPTED,
                0,
                0,
                false,
                null
            )
        )
        accountList.add(
            Account(
                0,
                identityId,
                "Committed account",
                "address02",
                "a02",
                TransactionStatus.COMMITTED,
                "",
                revealedAttributes,
                CredentialWrapper(credential,0),
                0,
                0,
                0,
                0,
                0,
                null,
                null,
                        ShieldedAccountEncryptionStatus.ENCRYPTED,
                0,
                0,
                false,
                null
            )
        )
        accountList.add(
            Account(
                0,
                identityId,
                "Received account",
                "address03",
                "a03",
                TransactionStatus.RECEIVED,
                "",
                revealedAttributes,
                CredentialWrapper(credential,0),
                0,
                0,
                0,
                0,
                0,
                null,
                null,
                ShieldedAccountEncryptionStatus.ENCRYPTED,
                0,
                0,
                false,
                null


            )
        )
        accountList.add(
            Account(
                0,
                identityId,
                "Absent account",
                "address04",
                "a04",
                TransactionStatus.ABSENT,
                "",
                revealedAttributes,
                CredentialWrapper(credential,0),
                0,
                0,
                0,
                0,
                0,
                null,
                null,
                        ShieldedAccountEncryptionStatus.ENCRYPTED,
                0,
                0,
                false,
                null

            )
        )
        accountRepository.insertAll(accountList)
    }

    private suspend fun createTransfers() {
        val account = accountRepository.findByAddress("address01")
        val accountId = account?.id ?: 0
        val transferList = ArrayList<Transfer>()
        val createAtBaseLine = OfflineMockInterceptor.initialTimestampSecs * 1000L
        val minuteInMillis = 60000
        val hourInMillis = minuteInMillis * 60

        transferList.add(
            Transfer(
                0,
                accountId,
                1000,
                59,
                "address01",
                "address02",
                1903773385,
                "",
                (createAtBaseLine - 1 * minuteInMillis),
                "t01",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null
            )
        )
        transferList.add(
            Transfer(
                0,
                accountId,
                2000,
                59,
                "address01",
                "address02_not_in_recipientlist",
                1903773385,
                "",
                (createAtBaseLine - 2 * minuteInMillis),
                "t02",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null
            )
        )
        transferList.add(
            Transfer(
                0,
                accountId,
                3000,
                59,
                "address01",
                "address02",
                1903773385,
                "",
                (createAtBaseLine - 3 * minuteInMillis),
                "t03",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null
            )
        )
        transferList.add(
            Transfer(
                0,
                accountId,
                4000,
                59,
                "address01",
                "address02",
                1903773385,
                "",
                (createAtBaseLine - 4 * minuteInMillis),
                "t04",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null

            )
        )
        transferList.add(
            Transfer(
                0,
                accountId,
                5000,
                59,
                "address01",
                "address02",
                1903773385,
                "",
                (createAtBaseLine - 5 * minuteInMillis),
                "t05",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null
            )
        )
        transferList.add(
            Transfer(
                0,
                accountId,
                6000,
                59,
                "address01",
                "address02",
                1903773385,
                "",
                (createAtBaseLine - 6 * minuteInMillis),
                "t06",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null

            )
        )
        transferList.add(
            Transfer(
                0,
                accountId,
                7000,
                59,
                "address01",
                "address02",
                1903773385,
                "",
                (createAtBaseLine - 7 * minuteInMillis),
                "t07",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null

            )
        )
        // Some more to test merging
        var timeStamp = (createAtBaseLine - 1 * hourInMillis)
        for (i in 1..10) {
            transferList.add(
                Transfer(
                    0,
                    accountId,
                    1000,
                    59,
                    "address01",
                    "Local",
                    1903773385,
                    "",
                    timeStamp,
                    "t01",
                    TransactionStatus.RECEIVED,
                    TransactionOutcome.Success,
                    TransactionType.TRANSFER,
                    null,
                    0,
                    null
                )
            )
            timeStamp -= 6 * hourInMillis
        }

        transferRepository.insertAll(transferList)
    }

    private suspend fun createRecipients() {
        val recipientLis = ArrayList<Recipient>()
        recipientLis.add(Recipient(0, "Carl1", "address01"))
        recipientLis.add(Recipient(0, "Sara", "address02"))
        recipientLis.add(Recipient(0, "Mohamed", "address03"))
        recipientLis.add(Recipient(0, "Salma", "address04"))
        recipientLis.add(Recipient(0, "Heba", "address05"))
        recipientLis.add(Recipient(0, "Hoda", "address06"))
        recipientLis.add(Recipient(0, "John", "address07"))
        recipientLis.add(Recipient(0, "Jane", "address08"))
        recipientLis.add(Recipient(0, "Brian", "address09"))
        recipientLis.add(Recipient(0, "Ronald", "address10"))
        recipientLis.add(Recipient(0, "James", "address11"))
        recipientLis.add(Recipient(0, "Brittany", "address12"))
        recipientLis.add(Recipient(0, "Jack", "address13"))
        recipientRepository.insertAll(recipientLis)
    }

    fun clearData() = viewModelScope.launch {
        identityRepository.deleteAll()
        accountRepository.deleteAll()
        transferRepository.deleteAll()
        recipientRepository.deleteAll()
    }

}