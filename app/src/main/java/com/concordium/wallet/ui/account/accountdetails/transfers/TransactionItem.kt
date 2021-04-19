package com.concordium.wallet.ui.account.accountdetails.transfers

import com.concordium.wallet.data.model.Transaction

class TransactionItem(var transaction: Transaction? = null) : AdapterItem {

    override fun getItemType(): AdapterItem.ItemType {
        return AdapterItem.ItemType.Item
    }
}