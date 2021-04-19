package com.concordium.wallet.ui.account.accountdetails.transfers

interface AdapterItem {
    enum class ItemType { Header, Item }

    fun getItemType(): ItemType
}