package com.concordium.wallet.ui.identity.identitydetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import kotlinx.android.synthetic.main.item_identity_attribute.view.*
import java.util.*
import kotlin.collections.ArrayList


class IdentityAttributeAdapter(private var data: SortedMap<String, String>) :
    RecyclerView.Adapter<IdentityAttributeAdapter.ItemViewHolder>() {

    var keys = ArrayList<String>(data.keys)

    class ItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val nameTextView: TextView = view.attribute_name_textview
        private val valueTextView: TextView = view.attribute_value_textview

        fun bind(name: String, value: String) {
            val attributeKeyValue = IdentityAttributeConverterUtil.convertAttributeValue(
                view.context,
                Pair(name, value)
            )
            nameTextView.text = attributeKeyValue.first
            valueTextView.text = attributeKeyValue.second
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_identity_attribute,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val name = keys[position]
        val value = data[name]
        value?.let {
            holder.bind(name, value)
        }

    }
}