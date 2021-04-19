package com.concordium.wallet.ui.recipient.recipientlist

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Recipient
import kotlinx.android.synthetic.main.item_recipient.view.*


class RecipientAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null
    private var data: List<Recipient> = emptyList()
    private var allData: List<Recipient> = emptyList()
    private var currentFilter = ""


    class ItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val rootLayout: View = view.root_layout
        private val nameTextView: TextView = view.recipient_name_textview
        private val addressTextView: TextView = view.recipient_address_textview

        fun bind(item: Recipient, onItemClickListener: OnItemClickListener?) {
            nameTextView.text = item.name
            addressTextView.text = item.address

            // Click
            if (onItemClickListener != null) {
                rootLayout.setOnClickListener {
                    onItemClickListener.onItemClicked(item)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_recipient,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data.get(position)
        (holder as ItemViewHolder).bind(item, onItemClickListener)
    }

    fun setData(data: List<Recipient>) {
        if (allData.isEmpty()) {
            this.data = data
            this.allData = data
            notifyDataSetChanged()
            return
        }

        // Compare list to find changes (use filtered list of new data if there is a filter active)
        //the diffCallback is flawed and didn't reveal newly created items
        //var newDataWithFilter = data
        //if (!TextUtils.isEmpty(currentFilter)) {
        //    newDataWithFilter = getFilteredList(data, currentFilter)
        //}
        //val diffCallback = RecipientDiffCallback(this.data, newDataWithFilter)
        //val diffResult = DiffUtil.calculateDiff(diffCallback)

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
            recipient.name.toLowerCase().contains(filterString.toLowerCase())
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

    //region DiffUtil
    //************************************************************

    class RecipientDiffCallback(
        oldEmployeeList: List<Recipient>,
        newEmployeeList: List<Recipient>
    ) : DiffUtil.Callback() {

        private val oldList: List<Recipient> = oldEmployeeList
        private val newList: List<Recipient> = newEmployeeList

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean {
            val oldItem: Recipient = oldList[oldItemPosition]
            val newItem: Recipient = newList[newItemPosition]
            return oldItem.name == newItem.name &&
                    oldItem.address == newItem.address
        }
    }

    //endregion

}