package com.concordium.wallet.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ItemIdentityBinding
import com.concordium.wallet.uicore.view.IdentityView

class IdentityAdapter : RecyclerView.Adapter<IdentityAdapter.ItemViewHolder>() {

    private var data: List<Identity> = emptyList()
    private var onItemClickListener: OnItemClickListener? = null

    inner class ItemViewHolder(val binding: ItemIdentityBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemIdentityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder) {
            val item = data[position]
            binding.identityView.setIdentityData(item)
            binding.identityView.setOnItemClickListener(object : IdentityView.OnItemClickListener {
                override fun onItemClicked(item: Identity) {
                    onItemClickListener?.onItemClicked(item)
                }
            })
        }
    }

    fun setData(data: List<Identity>) {
        this.data = data
        notifyDataSetChanged()
    }

    //region OnItemClickListener
    //************************************************************

    interface OnItemClickListener {
        fun onItemClicked(item: Identity)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    //endregion
}
