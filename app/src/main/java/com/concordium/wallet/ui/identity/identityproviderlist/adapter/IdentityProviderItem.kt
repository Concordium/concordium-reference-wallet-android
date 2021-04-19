package com.concordium.wallet.ui.identity.identityproviderlist.adapter

import com.concordium.wallet.data.model.IdentityProvider

class IdentityProviderItem(val identityProvider: IdentityProvider) : AdapterItem {
    override fun getItemType(): ItemType {
        return ItemType.Item
    }
}
