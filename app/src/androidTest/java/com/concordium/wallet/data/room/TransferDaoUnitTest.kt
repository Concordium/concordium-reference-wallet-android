package com.concordium.wallet.data.room

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
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
class TransferDaoUnitTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var transferDao: TransferDao
    private lateinit var db: WalletDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java).build()
        transferDao = db.transferDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun createTransferTest() {
        val transfer = Transfer(
            1,
            1,
            100,
            20,
            "0",
            "0",
            1901176193,
            "",
            1585643393,
            "0",
            TransactionStatus.UNKNOWN,
            TransactionOutcome.UNKNOWN,
            TransactionType.TRANSFER,
            null,
            0,
            null
        )

        runBlocking { transferDao.insert(transfer) }
        val listLiveData = transferDao.getAllAsLiveData()
        val list = listLiveData.getOrAwaitValue()

        assertEquals(1, list.size)
        assertEquals(transfer.amount, list[0].amount)
    }

    @Test
    @Throws(Exception::class)
    fun transferOrderingNewestFirstTest() {
        val transfer1 = createTransferWithCreateAt(1)
        val transfer2 = createTransferWithCreateAt(3)
        val transfer3 = createTransferWithCreateAt(2)

        runBlocking { transferDao.insert(transfer1, transfer2, transfer3) }
        val listLiveData = transferDao.getAllAsLiveData()
        val list = listLiveData.getOrAwaitValue()

        assertEquals(3, list.size)
        assertEquals(3, list[0].createdAt)
        assertEquals(2, list[1].createdAt)
        assertEquals(1, list[2].createdAt)
    }

    private fun createTransferWithCreateAt(createdAt: Long): Transfer {
        return Transfer(
            0,
            1,
            100,
            20,
            "0",
            "0",
            1901176193,
            "",
            createdAt,
            "0",
            TransactionStatus.UNKNOWN,
            TransactionOutcome.UNKNOWN,
            TransactionType.TRANSFER,
            null,
            0,
            null
        )
    }
}
