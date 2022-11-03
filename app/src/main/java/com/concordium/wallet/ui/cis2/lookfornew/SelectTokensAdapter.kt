package com.concordium.wallet.ui.cis2.lookfornew

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.ItemTokenBinding

class SelectTokensAdapter(private val context: Context, var dataSet: Array<Token>) : RecyclerView.Adapter<SelectTokensAdapter.ViewHolder>() {
    private var tokenClickListener: TokenClickListener? = null

    inner class ViewHolder(val binding: ItemTokenBinding): RecyclerView.ViewHolder(binding.root)

    interface TokenClickListener {
        fun onRowClick(token: Token)
        fun onCheckBoxClick(token: Token)
    }

    fun setTokenClickListener(tokenClickListener: TokenClickListener) {
        this.tokenClickListener = tokenClickListener
    }

    override fun getItemCount() = dataSet.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTokenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val token = dataSet[position]

        if (!token.imageUrl.isNullOrBlank())
            Glide.with(context).load(token.imageUrl).into(holder.binding.tokenIcon)

        holder.binding.title.text = token.id.toString()
        holder.binding.subTitle.text = token.token

        holder.binding.root.setOnClickListener {
            tokenClickListener?.onRowClick(token)
        }
    }
}
