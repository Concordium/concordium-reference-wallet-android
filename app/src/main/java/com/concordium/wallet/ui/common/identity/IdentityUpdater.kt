package com.concordium.wallet.ui.common.identity

import android.app.Application
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.util.Log
import kotlinx.coroutines.*
import java.io.FileNotFoundException
import java.net.URL

class IdentityUpdater(val application: Application, private val viewModelScope: CoroutineScope) {

    private val gson = App.appCore.gson
    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository

    private var updateListener: UpdateListener? = null
    private var run = true

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
    }

    interface UpdateListener {
        fun onError(identity: Identity, account: Account?)
        fun onDone()
    }

    fun setUpdateListener(updateListener: UpdateListener) {
        this.updateListener = updateListener
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

                    val identityTokenContainer = gson.fromJson<IdentityTokenContainer>(resp, IdentityTokenContainer::class.java)
                    val newStatus = identityTokenContainer.status
                    if (newStatus != IdentityStatus.PENDING) {
                        identity.status = identityTokenContainer.status
                        identity.detail = identityTokenContainer.detail
                        if (newStatus == IdentityStatus.DONE && identityTokenContainer.token != null) {
                            val token = identityTokenContainer.token
                            val identityContainer = token.identityObject
                            val accountCredentialWrapper = token.credential
                            val accountAddress = token.accountAddress
                            identity.identityObject = identityContainer.value
                            identityRepository.update(identity)
                            val account = accountRepository.getAllByIdentityId(identity.id).firstOrNull()
                            account?.let {
                                if (it.address == accountAddress) {
                                    //it.credential = CredentialWrapper(RawJson(gson.toJson(CredentialContentWrapper(accountCredentialWrapper.value))), accountCredentialWrapper.v) //Make up for protocol inconsistency
                                    it.credential = accountCredentialWrapper
                                    if (identityTokenContainer.status == IdentityStatus.DONE) {
                                        account.transactionStatus = TransactionStatus.FINALIZED
                                    } else if (identityTokenContainer.status == IdentityStatus.ERROR) {
                                        account.transactionStatus = TransactionStatus.ABSENT
                                    }
                                    accountRepository.update(account)
                                }
                            }
                        } else if (newStatus == IdentityStatus.ERROR) {

                            identityRepository.update(identity)
                            val account = accountRepository.getAllByIdentityId(identity.id).firstOrNull()
                            account?.let { accountRepository.delete(it) }
                            withContext(Dispatchers.Main) {
                                updateListener?.onError(identity, account)
                            }
                        }
                    } else {
                        hasMorePending = true
                    }
                } catch (e: FileNotFoundException) {
                    Log.e("Identity backend request failed", e)
                    hasMorePending = true
                }
            }
        }
        return hasMorePending
    }
}