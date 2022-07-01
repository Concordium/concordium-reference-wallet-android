package com.concordium.wallet.ui.account.newaccountidentityattributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import com.concordium.wallet.databinding.ItemIdentityAttributeCheckableBinding

class IdentityAttributeAdapter : RecyclerView.Adapter<IdentityAttributeAdapter.ItemViewHolder>() {
    private var data: List<SelectableIdentityAttribute> = emptyList()
    private var onItemClickListener: OnItemClickListener? = null

    inner class ItemViewHolder(val binding: ItemIdentityAttributeCheckableBinding): RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemIdentityAttributeCheckableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder) {
            val item = data[position]

            val attributeKeyValue = IdentityAttributeConverterUtil.convertAttributeValue(
                binding.root.context,
                Pair(item.name, item.value)
            )

            binding.attributeNameTextview.text = attributeKeyValue.first
            binding.attributeValueTextview.text = attributeKeyValue.second
            //Set the listener before assigning default value to the checked state
            binding.attributeCheckbox.setOnCheckedChangeListener(null)
            binding.attributeCheckbox.isChecked = item.isSelected

            binding.attributeCheckbox.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                onItemClickListener?.onCheckedChanged(item)
            }

            // Click
            binding.rootLayout.setOnClickListener {
                binding.attributeCheckbox.isChecked = !binding.attributeCheckbox.isChecked
                onItemClickListener?.onItemClicked(item)
            }
        }
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