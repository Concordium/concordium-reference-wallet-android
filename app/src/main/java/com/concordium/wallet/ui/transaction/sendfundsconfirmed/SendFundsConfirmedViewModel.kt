package com.concordium.wallet.ui.transaction.sendfundsconfirmed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.Transfer


class SendFundsConfirmedViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var transfer: Transfer
    lateinit var recipient: Recipient

    fun initialize(
        transfer: Transfer,
        recipient: Recipient
    ) {
        this.transfer = transfer
        this.recipient = recipient
    }
}