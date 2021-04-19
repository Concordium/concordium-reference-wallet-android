package com.concordium.wallet.ui.account.accountdetails.transfers

class HeaderItem(var title: String = "") : AdapterItem {

    override fun getItemType(): AdapterItem.ItemType {
        return AdapterItem.ItemType.Header
    }

}