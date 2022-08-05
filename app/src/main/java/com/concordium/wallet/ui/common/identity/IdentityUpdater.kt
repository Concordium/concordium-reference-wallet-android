package com.concordium.wallet.ui.common.identity

import android.app.Application
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.model.IdentityTokenContainer
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.util.Log
import kotlinx.coroutines.*
import java.io.FileNotFoundException
import java.net.URL

class IdentityUpdater(val application: Application, private val viewModelScope: CoroutineScope) {
    private val gson = App.appCore.gson
    private val identityRepository: IdentityRepository
    private val recipientRepository: RecipientRepository
    private val database: WalletDatabase = WalletDatabase.getDatabase(application)

    private var updateListener: UpdateListener? = null
    private var run = true

    init {
        val identityDao = database.identityDao()
        identityRepository = IdentityRepository(identityDao)
        val recipientDao = database.recipientDao()
        recipientRepository = RecipientRepository(recipientDao)
    }

    interface UpdateListener {
        fun onError(identity: Identity)
        fun onDone()
    }

    fun dispose() {
        // Coroutines is disposed/cancelled when in viewModelScope
    }

    fun stop() {
        run = false
        updateListener = null
        Log.d("Poll for identity status stopped")
    }

    fun checkPendingIdentities(updateListener: UpdateListener) {
        Log.d("Poll for identity status started")
        this.updateListener = updateListener
        run = true
        viewModelScope.launch(Dispatchers.Default) {
            var hasMorePending = true
            while (isActive && hasMorePending && run) {
                hasMorePending = pollForIdentityStatus()
                delay(5000)
            }
            withContext(Dispatchers.Main) {
                updateListener.onDone()
            }
            Log.d("Poll for identity status done")
        }
    }

    private suspend fun pollForIdentityStatus(): Boolean {
        var hasMorePending = false
        Log.d("Poll for identity status")
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
                            updateListener?.onError(identity)
                        }
                    } else {
                        hasMorePending = true
                    }
                } catch (e: FileNotFoundException) {
                    Log.e("Identity backend request failed", e)
                    hasMorePending = true
                } catch (e: Exception) {
                    Log.e("Identity backend failure", e)
                    e.printStackTrace()
                    hasMorePending = true
                }
            }
        }
        return hasMorePending
    }
}
