package com.concordium.wallet.ui.account.newaccountidentityattributes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import kotlinx.android.synthetic.main.item_identity_attribute_checkable.view.*


class IdentityAttributeAdapter :
    RecyclerView.Adapter<IdentityAttributeAdapter.ItemViewHolder>() {

    private var data: List<SelectableIdentityAttribute> = emptyList()
    private var onItemClickListener: OnItemClickListener? = null

    class ItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val rootLayout: View = view.root_layout
        private val checkBox: CheckBox = view.attribute_checkbox
        private val nameTextView: TextView = view.attribute_name_textview
        private val valueTextView: TextView = view.attribute_value_textview

        fun bind(
            item: SelectableIdentityAttribute,
            onItemClickListener: OnItemClickListener?
        ) {
            val attributeKeyValue = IdentityAttributeConverterUtil.convertAttributeValue(
                view.context,
                Pair(item.name, item.value)
            )

            nameTextView.text = attributeKeyValue.first
            valueTextView.text = attributeKeyValue.second
            //Set the listener before assigning default value to the checked state
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = item.isSelected

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                onItemClickListener?.onCheckedChanged(item)
            }

            // Click
            rootLayout.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
                onItemClickListener?.onItemClicked(item)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_identity_attribute_checkable,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(data[position], onItemClickListener)
    }

    fun setData(data: List<SelectableIdentityAttribute>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun getCheckedAttributes(): List<SelectableIdentityAttribute> {
        return data.filter(SelectableIdentityAttribute::isSelected)
    }

    //region OnItemClickListener
    //************************************************************

    interface OnItemClickListener {
        fun onItemClicked(item: SelectableIdentityAttribute)
        fun onCheckedChanged(item: SelectableIdentityAttribute)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    //endregion
}