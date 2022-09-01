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
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.identity.IdentityUpdater

class MainViewModel(application: Application) : AndroidViewModel(application) {
    enum class State {
        AccountOverview
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

    init {
        identityRepository = IdentityRepository(WalletDatabase.getDatabase(application).identityDao())
    }

    fun initialize() {
        try {
            WalletDatabase.getDatabase(getApplication()).openHelper.readableDatabase?.version.toString()
        }
        catch(e: Exception){
            databaseVersionAllowed = false
        }
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
        identityUpdater.checkPendingIdentities()
    }

    fun stopIdentityUpdate() {
        identityUpdater.stop()
    }
}
