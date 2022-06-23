package com.concordium.wallet.ui.account.newaccountidentityattributes

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import com.concordium.wallet.ui.account.common.NewAccountViewModel

class NewAccountIdentityAttributesViewModel(application: Application) : NewAccountViewModel(application) {

    private val _identityAttributeListLiveData =
        MutableLiveData<List<SelectableIdentityAttribute>>()
    val identityAttributeListLiveData: LiveData<List<SelectableIdentityAttribute>>
        get() = _identityAttributeListLiveData


    override fun initialize(accountName: String, identity: Identity) {
        super.initialize(accountName, identity)
        // Avoid clearing checked state if this has already been initialized
        if (_identityAttributeListLiveData.value == null) {
            _identityAttributeListLiveData.value = getChosenAttributeList()
        }
    }

    private fun getChosenAttributeList(): ArrayList<SelectableIdentityAttribute> {
        val attributeList = ArrayList<SelectableIdentityAttribute>()
        val attributes = identity.identityObject!!.attributeList.chosenAttributes
        attributes.toSortedMap().forEach { (key, value) ->
            if (IdentityAttributeConverterUtil.visibleIdentityAttributes.contains(key)) {
                attributeList.add(SelectableIdentityAttribute(key, value, false))
            }
        }
        return attributeList
    }


}