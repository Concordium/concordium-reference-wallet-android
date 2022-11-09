package com.concordium.wallet.ui.cis2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.ItemTokenBinding
import com.concordium.wallet.util.UnitConvertUtil

class TokensAdapter(private val context: Context, private val showCheckBox: Boolean, private val showLastRow: Boolean, var dataSet: Array<Token>) : RecyclerView.Adapter<TokensAdapter.ViewHolder>() {
    private var tokenClickListener: TokenClickListener? = null
    private val iconSize: Int get() = UnitConvertUtil.convertDpToPixel(context.resources.getDimension(R.dimen.list_item_height))

    inner class ViewHolder(val binding: ItemTokenBinding): RecyclerView.ViewHolder(binding.root)

    interface TokenClickListener {
        fun onRowClick(token: Token)
        fun onCheckBoxClick(token: Token)
    }

    fun setTokenClickListener(tokenClickListener: TokenClickListener) {
        this.tokenClickListener = tokenClickListener
    }

    override fun getItemCount() = if (showLastRow) dataSet.size + 1 else dataSet.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTokenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == itemCount - 1 && showLastRow) {
            holder.binding.addMore.visibility = View.VISIBLE
            holder.binding.content.visibility = View.GONE
            return
        } else {
            holder.binding.addMore.visibility = View.GONE
            holder.binding.content.visibility = View.VISIBLE
        }

        val token = dataSet[position]

        if (!token.imageUrl.isNullOrBlank())
            Glide.with(context)
                .load(token.imageUrl)
                .placeholder(R.drawable.ic_token_loading_image)
                .override(iconSize)
                .fitCenter()
                .error(R.drawable.ic_token_no_image)
                .into(holder.binding.tokenIcon)

        holder.binding.title.text = token.id.toString()
        holder.binding.subTitle.text = token.token

        if (showCheckBox) {
            holder.binding.selection.isChecked = token.isSelected
        } else {
            holder.binding.selection.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener {
            tokenClickListener?.onRowClick(token)
        }
        holder.binding.selection.setOnClickListener {
            tokenClickListener?.onCheckBoxClick(token)
        }
    }
}
