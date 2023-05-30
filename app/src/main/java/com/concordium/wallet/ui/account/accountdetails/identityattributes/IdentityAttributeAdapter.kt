package com.concordium.wallet.ui.account.accountdetails.identityattributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.model.IdentityAttribute
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import com.concordium.wallet.databinding.ItemAccountIdentityAttributeBinding

class IdentityAttributeAdapter(
    private var data: List<IdentityAttribute>,
    private val providerName: String
) :
    RecyclerView.Adapter<IdentityAttributeAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemAccountIdentityAttributeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder) {
            with(data[position]) {
                val attributeKeyValue = IdentityAttributeConverterUtil.convertAttributeValue(
                    binding.root.context,
                    Pair(name, value)
                )
                binding.attributeNameTextview.text = attributeKeyValue.first
                binding.attributeValueTextview.text = attributeKeyValue.second
                binding.providerNameTextview.text = providerName
            }
        }
    }

    inner class ItemViewHolder(val binding: ItemAccountIdentityAttributeBinding) :
        RecyclerView.ViewHolder(binding.root)
}
