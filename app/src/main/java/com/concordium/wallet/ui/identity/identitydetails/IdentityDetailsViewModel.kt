package com.concordium.wallet.ui.identity.identitydetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.model.IdentityContainer
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase

class IdentityDetailsViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var identity: Identity

    private val identityRepository: IdentityRepository

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
    }

    fun initialize(identity: Identity) {
        this.identity = identity
    }

    suspend fun removeIdentity(identity: Identity) {
        identityRepository.delete(identity)
    }
}