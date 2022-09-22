package com.concordium.wallet.ui.common.identity

import android.app.Application
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.model.IdentityTokenContainer
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.net.URL

class IdentityUpdater(val application: Application, private val viewModelScope: CoroutineScope) {
    private val gson = App.appCore.gson
    private val identityRepository: IdentityRepository
    private val recipientRepository: RecipientRepository
    private var run = false

    init {
        val database = WalletDatabase.getDatabase(application)
        identityRepository = IdentityRepository(database.identityDao())
        recipientRepository = RecipientRepository(database.recipientDao())
    }

    fun stop() {
        run = false
    }

    fun checkPendingIdentities() {
        if (run)
            return
        run = true
        viewModelScope.launch(Dispatchers.Default) {
            while (run) {
                pollForIdentityStatus()
                delay(5000)
            }
        }
    }

    private suspend fun pollForIdentityStatus() {
        for (identity in identityRepository.getAll()) {
            if (identity.status == IdentityStatus.PENDING) {
                try {
                    val resp = URL(identity.codeUri).readText()
                    Log.d("Identity poll response: $resp")
                    val identityTokenContainer = gson.fromJson(resp, IdentityTokenContainer::class.java)
                    val newStatus = if (BuildConfig.FAIL_IDENTITY_CREATION) IdentityStatus.ERROR else identityTokenContainer.status
                    if (newStatus != IdentityStatus.PENDING) {
                        identity.status = identityTokenContainer.status
                        identity.detail = identityTokenContainer.detail
                        if (newStatus == IdentityStatus.DONE && identityTokenContainer.token != null) {
                            val token = identityTokenContainer.token
                            val identityContainer = token.identityObject
                            identity.identityObject = identityContainer.value
                            identityRepository.update(identity)
                        } else if (newStatus == IdentityStatus.ERROR) {
                            identityRepository.update(identity)
                        }
                        if (App.appCore.newIdentities[identity.id] == null)
                            App.appCore.newIdentities[identity.id] = identity
                    }
                } catch (e: FileNotFoundException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
