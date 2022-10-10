package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account

class SendTokenViewModel(application: Application) : AndroidViewModel(application) {
    var token: Token? = null
    lateinit var account: Account

    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
}
