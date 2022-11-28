package com.concordium.wallet.ui.cis2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.ItemTokenAddBinding
import com.concordium.wallet.util.UnitConvertUtil

class TokensAddAdapter(private val context: Context, private val showCheckBox: Boolean, var dataSet: Array<Token>) : RecyclerView.Adapter<TokensAddAdapter.ViewHolder>() {
    private var tokenClickListener: TokenClickListener? = null
    private val iconSize: Int get() = UnitConvertUtil.convertDpToPixel(context.resources.getDimension(R.dimen.list_item_height))

    inner class ViewHolder(val binding: ItemTokenAddBinding): RecyclerView.ViewHolder(binding.root)

    interface TokenClickListener {
        fun onRowClick(token: Token)
        fun onCheckBoxClick(token: Token)
    }

    fun setTokenClickListener(tokenClickListener: TokenClickListener) {
        this.tokenClickListener = tokenClickListener
    }

    override fun getItemCount() = dataSet.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTokenAddBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val token = dataSet[position]

        token.tokenMetadata?.let { tokenMetadata ->
            if (tokenMetadata.thumbnail.url.isNotBlank()) {
                Glide.with(context)
                    .load(tokenMetadata.thumbnail.url)
                    .placeholder(R.drawable.ic_token_loading_image)
                    .override(iconSize)
                    .fitCenter()
                    .error(R.drawable.ic_token_no_image)
                    .into(holder.binding.tokenIcon)
            } else if (tokenMetadata.thumbnail.url == "none") {
                holder.binding.tokenIcon.setImageResource(R.drawable.ic_token_no_image)
            }
            holder.binding.title.text = tokenMetadata.name
            holder.binding.subTitle.text = tokenMetadata.description
        }

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
