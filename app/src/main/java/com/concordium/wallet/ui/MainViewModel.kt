package com.concordium.wallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.onboarding.data.OnboardingRepository
import com.concordium.wallet.ui.common.identity.IdentityUpdater
import kotlinx.coroutines.launch

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

    private val _showTermsAndConditions = MutableLiveData(false)
    val showTermsAndConditions: LiveData<Boolean>
        get() = _showTermsAndConditions
    lateinit var onboardingRepository: OnboardingRepository

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

    fun startIdentityUpdate() {
        identityUpdater.checkPendingIdentities()
    }

    fun stopIdentityUpdate() {
        identityUpdater.stop()
    }

    fun checkTermsAndConditions() {
        viewModelScope.launch {
            val localVersion = onboardingRepository.getLocalAcceptedTermsAndConditionsVersion()
            onboardingRepository.getRemoteAcceptedTermsAndConditionsVersion()
                .onSuccess { result ->
                    if (result.version != localVersion) {
                        _showTermsAndConditions.value = true
                    }
                }
        }
    }

    fun onTermsAndConditionsOpen() {
        _showTermsAndConditions.value = false
    }
}
