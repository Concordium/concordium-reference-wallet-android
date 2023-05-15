package com.concordium.wallet.ui.account.accountsoverview

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.room.AccountWithIdentity

class AccountAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var data: List<AccountWithIdentity> = emptyList()
    private var onItemClickListener: AccountItemView.OnItemClickListener? = null

    class ItemViewHolder(val view: AccountItemView) : RecyclerView.ViewHolder(view) {

        fun bind(item: AccountWithIdentity, onItemClickListener: AccountItemView.OnItemClickListener?) {
            view.setAccount(item)
            view.setOnItemClickListener(onItemClickListener)
        }
    }

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(AccountItemView(parent.context, null))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ItemViewHolder).bind(data[position], onItemClickListener)
    }

    fun setData(data: List<AccountWithIdentity>) {
        this.data = data
        notifyDataSetChanged()
    }

    //region OnItemClickListener
    //************************************************************

    fun setOnItemClickListener(onItemClickListener: AccountItemView.OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    //endregion
}