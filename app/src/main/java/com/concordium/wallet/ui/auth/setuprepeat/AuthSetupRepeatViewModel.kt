package com.concordium.wallet.ui.auth.setuprepeat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.authentication.AuthenticationManager
import com.concordium.wallet.core.authentication.Session

class AuthSetupRepeatViewModel(application: Application) : AndroidViewModel(application) {

//App.appCore.getCurrentAuthenticationManager()
    private val session: Session = App.appCore.session

    private val _finishScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishScreenLiveData

    fun initialize() {

    }

    fun checkPassword(password: String) {
        val isEqual = session.checkPassword(password)
        _finishScreenLiveData.value = Event(isEqual)
    }

}