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
class IdentityDaoUnitTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var identityDao: IdentityDao
    private lateinit var db: WalletDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java).build()
        identityDao = db.identityDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun createIdentityTest() {
        val identityProviderInfo = IdentityProviderInfo(
            0,
            IdentityProviderDescription("description", "ID Provider", "url"),
            "",
            ""
        )
        val identityProvider =
            IdentityProvider(
                identityProviderInfo,
                HashMap<String, ArsInfo>(),
                IdentityProviderMetaData("", "", null)
            )
        val pubInfoForIP = PubInfoForIp("", RawJson("{}"), "")
        val preIdentityObject =
            PreIdentityObject(
                RawJson("{}"), pubInfoForIP, "",
                RawJson("{}"), "",
                RawJson("{}"), ""
            )
        val identityObject = IdentityObject(
            AttributeList(HashMap(), "203012", 255, "20200101"),
            preIdentityObject,
            RawJson("{}")
        )
        val identity =
            Identity(0, "identity name", "", "", "", 0, identityProvider, identityObject, "")

        runBlocking { identityDao.insert(identity) }
        val listLiveData = identityDao.getAllAsLiveData()
        val list = listLiveData.getOrAwaitValue()

        assertEquals(1, list.size)
        assertEquals(
            identity.identityProvider.ipInfo.ipDescription.description,
            list[0].identityProvider.ipInfo.ipDescription.description
        )
    }
}