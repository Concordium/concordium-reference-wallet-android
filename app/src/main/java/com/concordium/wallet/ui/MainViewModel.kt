package com.concordium.wallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.identity.IdentityUpdater
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityErrorData
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    enum class State {
        AccountOverview, Backup, More
    }

    private val identityRepository: IdentityRepository
    private val identityUpdater = IdentityUpdater(application, viewModelScope)
    private val session: Session = App.appCore.session

    var databaseVersionAllowed = true

    private val _newFinalizedAccountLiveData = MutableLiveData<String>()
    val newFinalizedAccountLiveData: LiveData<String>
        get() = _newFinalizedAccountLiveData

    private val _titleLiveData = MutableLiveData<String>()
    val titleLiveData: LiveData<String>
        get() = _titleLiveData

    private val _stateLiveData = MutableLiveData<State>()
    val stateLiveData: LiveData<State>
        get() = _stateLiveData

    private val _identityErrorLiveData = MutableLiveData<IdentityErrorData>()
    val identityErrorLiveData: LiveData<IdentityErrorData>
        get() = _identityErrorLiveData

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
    }

    fun initialize() {
        try {
            WalletDatabase.getDatabase(getApplication()).openHelper.readableDatabase?.version.toString()
        }
        catch(e: Exception){
            databaseVersionAllowed = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        identityUpdater.stop()
    }

    fun setTitle(title: String) {
        _titleLiveData.value = title
    }

    fun setState(state: State) {
        _stateLiveData.value = state
    }

    fun setInitialStateIfNotSet() {
        if (_stateLiveData.value == null) {
            _stateLiveData.value = State.AccountOverview
        }
    }

    fun shouldShowAuthentication(): Boolean {
        if (session.isLoggedIn.value != null) {
            return session.isLoggedIn.value == false
        }
        return true
    }

    fun shouldShowTerms(): Boolean {
        val hashNew = App.appContext.getString(R.string.terms_text).hashCode()
        val hashOld = session.getTermsHashed()
        return hashNew != hashOld
    }

    fun startIdentityUpdate() {
        val updateListener = object: IdentityUpdater.UpdateListener {
            override fun onError(identity: Identity) {
                viewModelScope.launch {
                    val isFirst = isFirst(identityRepository.getCount())
                    _identityErrorLiveData.value = IdentityErrorData(identity, isFirst)
                }
            }
            override fun onDone() {
            }
        }
        identityUpdater.checkPendingIdentities(updateListener)
    }

    fun stopIdentityUpdate() {
        identityUpdater.stop()
    }

    private fun isFirst(identityCount: Int): Boolean {
        // If we are in the process of creating the first identity, there will be one identity saved at this point
        return (identityCount <= 1)
    }
}
