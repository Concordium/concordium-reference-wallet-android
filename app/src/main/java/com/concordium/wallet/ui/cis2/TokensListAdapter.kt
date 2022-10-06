package com.concordium.wallet.ui.cis2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ItemChooseAccountBinding
import com.concordium.wallet.databinding.ItemTokenBinding
import java.net.URI

class TokensListAdapter(private val context: Context, var arrayList: Array<Token>) : BaseAdapter() {
    private var tokenClickListener: TokenClickListener? = null

    fun interface TokenClickListener {
        fun onClick(token: Token)
    }

    fun setTokenClickListener(tokenClickListener: TokenClickListener) {
        this.tokenClickListener = tokenClickListener
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val binding: ItemTokenBinding

        if (convertView == null) {
            binding = ItemTokenBinding.inflate(LayoutInflater.from(context), parent, false)
            holder = ViewHolder(binding)
            holder.binding.root.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        val token = arrayList[position]
        //holder.binding.tokenIcon.setImageURI()
        holder.binding.tokenName.text = token.name

        holder.binding.root.setOnClickListener {
            tokenClickListener?.onClick(token)
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

    private inner class ViewHolder(val binding: ItemTokenBinding)
}
