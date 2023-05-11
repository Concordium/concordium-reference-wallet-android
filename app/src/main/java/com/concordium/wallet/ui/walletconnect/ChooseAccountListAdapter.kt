package com.concordium.wallet.ui.walletconnect

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ItemChooseAccountBinding

class ChooseAccountListAdapter(
    private val context: Context,
    var arrayList: Array<AccountWithIdentity>
) : BaseAdapter() {
    private var chooseAccountClickListener: ChooseAccountClickListener? = null

    fun interface ChooseAccountClickListener {
        fun onClick(accountWithIdentity: AccountWithIdentity)
    }

    fun setChooseAccountClickListener(chooseAccountClickListener: ChooseAccountClickListener) {
        this.chooseAccountClickListener = chooseAccountClickListener
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val binding: ItemChooseAccountBinding

        if (convertView == null) {
            binding = ItemChooseAccountBinding.inflate(LayoutInflater.from(context), parent, false)
            holder = ViewHolder(binding)
            holder.binding.root.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        val accountWithIdentity = arrayList[position]
        holder.binding.accountName.text = accountWithIdentity.account.name
        holder.binding.identityName.text = accountWithIdentity.identity.name
        holder.binding.total.text =
            CurrencyUtil.formatGTU(accountWithIdentity.account.totalUnshieldedBalance, true)
        holder.binding.atDisposal.text = CurrencyUtil.formatGTU(
            accountWithIdentity.account.getAtDisposalWithoutStakedOrScheduled(accountWithIdentity.account.totalUnshieldedBalance),
            true
        )

        holder.binding.root.setOnClickListener {
            chooseAccountClickListener?.onClick(accountWithIdentity)
        }

        return holder.binding.root
    }

    override fun getCount(): Int {
        return arrayList.size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    private inner class ViewHolder(val binding: ItemChooseAccountBinding)
}
