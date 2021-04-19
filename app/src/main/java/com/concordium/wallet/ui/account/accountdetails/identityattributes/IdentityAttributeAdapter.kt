package com.concordium.wallet.ui.account.accountdetails.identityattributes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityAttribute
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import kotlinx.android.synthetic.main.item_account_identity_attribute.view.*

class IdentityAttributeAdapter(
    private var data: List<IdentityAttribute>,
    val providerName: String
) :
    RecyclerView.Adapter<IdentityAttributeAdapter.ItemViewHolder>() {

    class ItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val attributeNameTextView: TextView = view.attribute_name_textview
        private val attributeValueTextView: TextView = view.attribute_value_textview
        private val providerNameTextView: TextView = view.provider_name_textview

        fun bind(
            item: IdentityAttribute,
            providerName: String
        ) {
            val attributeKeyValue = IdentityAttributeConverterUtil.convertAttributeValue(
                view.context,
                Pair(item.name, item.value)
            )
            attributeNameTextView.text = attributeKeyValue.first
            attributeValueTextView.text = attributeKeyValue.second
            providerNameTextView.text = providerName
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_account_identity_attribute,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(data[position], providerName)
    }
}