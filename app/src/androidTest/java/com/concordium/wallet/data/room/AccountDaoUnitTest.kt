package com.concordium.wallet.data.room

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.concordium.wallet.data.model.*
import com.concordium.wallet.getOrAwaitValue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

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
            0,
            0,
            0,
            0,
            0,
            null,
            null

        )

        runBlocking { accountDao.insert(account) }
        val listLiveData = accountDao.getAllAsLiveData()
        val list = listLiveData.getOrAwaitValue()

        assertEquals(1, list.size)
        assertEquals(account.name, list[0].name)
        assertEquals(2, list[0].revealedAttributes.size)
    }
}
