package com.concordium.wallet.data.room

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.concordium.wallet.data.model.*
import com.concordium.wallet.getOrAwaitValue
import com.concordium.wallet.util.toBigInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.math.BigInteger

@RunWith(AndroidJUnit4::class)
class AccountDaoUnitTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var accountDao: AccountDao
    private lateinit var db: WalletDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java).build()
        accountDao = db.accountDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun createAccountTest() {
        val revealedAttributes = ArrayList<IdentityAttribute>().apply {
            add(IdentityAttribute("name1", "value1"))
            add(IdentityAttribute("name2", "value2"))
        }
        val account = Account(
            1,
            1,
            "accountName",
            "0",
            "0",
            TransactionStatus.UNKNOWN,
            "",
            revealedAttributes,
            CredentialWrapper(RawJson("{}"), 1),
            "115792089237316195423570985008687907853269984665640564039457584007913129639935".toBigInteger(),
            BigInteger.ZERO,
            BigInteger.ZERO,
            BigInteger.ZERO,
            BigInteger.ZERO,
            null,
            null,
            ShieldedAccountEncryptionStatus.ENCRYPTED,
            BigInteger.ZERO,
            BigInteger.ZERO,
            false,
            null,
            null,
            null,
            null, 0, 0
        )

        runBlocking { accountDao.insert(account) }
        val listLiveData = accountDao.getAllAsLiveData()
        val list = listLiveData.getOrAwaitValue()

        assertEquals(1, list.size)
        assertEquals(account.name, list[0].name)
        assertEquals(2, list[0].revealedAttributes.size)
        assertEquals(account.finalizedBalance, list[0].finalizedBalance)
    }
}
