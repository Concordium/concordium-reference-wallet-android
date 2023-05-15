package com.concordium.wallet.ui.recipient.recipientlist

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.databinding.ItemRecipientBinding
import java.util.*

class RecipientAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null
    private var data: List<Recipient> = emptyList()
    private var allData: List<Recipient> = emptyList()
    private var currentFilter = ""

    inner class ItemViewHolder(val binding: ItemRecipientBinding): RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemRecipientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        with(holder as ItemViewHolder) {
            val item = data[position]
            binding.recipientNameTextview.text = item.name
            binding.recipientAddressTextview.text = item.address

            // Click
            if (onItemClickListener != null) {
                binding.rootLayout.setOnClickListener {
                    onItemClickListener?.onItemClicked(item)
                }
            }
        }
    }

    fun setData(data: List<Recipient>) {
        if (allData.isEmpty()) {
            this.data = data
            this.allData = data
            notifyDataSetChanged()
            return
        }

        // Update internal lists with the new data
        this.allData = data
        if (!TextUtils.isEmpty(currentFilter)) {
            this.data = getFilteredList(data, currentFilter)
        } else {
            this.data = data
        }

        // Perform change animations
        //diffResult.dispatchUpdatesTo(this)
        notifyDataSetChanged()
    }

    fun get(position: Int): Recipient {
        return data[position]
    }

    fun filter(filterString: String?) {
        currentFilter = filterString ?: ""
        data = getFilteredList(allData, currentFilter)
        notifyDataSetChanged()
    }

    private fun getFilteredList(allData: List<Recipient>, filterString: String): List<Recipient> {
        return allData.filter { recipient ->
            recipient.name.lowercase(Locale.getDefault()).contains(filterString.lowercase(Locale.getDefault()))
        }
    }

    //region OnItemClickListener
    //************************************************************

    interface OnItemClickListener {
        fun onItemClicked(item: Recipient)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    //endregion
}
