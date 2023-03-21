package com.concordium.wallet.ui.cis2

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.util.CurrencyUtilImpl
import com.concordium.wallet.databinding.ItemTokenAccountDetailsBinding
import com.concordium.wallet.util.UnitConvertUtil

class TokensAccountDetailsAdapter(
    private val context: Context,
    private val isFungible: Boolean,
    private val showManageInfo: Boolean,
    var dataSet: Array<Token>
) : RecyclerView.Adapter<TokensAccountDetailsAdapter.ViewHolder>() {
    private var tokenClickListener: TokenClickListener? = null
    private val iconSize: Int
        get() = UnitConvertUtil.convertDpToPixel(
            context.resources.getDimension(
                R.dimen.list_item_height
            )
        )

    inner class ViewHolder(val binding: ItemTokenAccountDetailsBinding) :
        RecyclerView.ViewHolder(binding.root)

    interface TokenClickListener {
        fun onRowClick(token: Token)
        fun onCheckBoxClick(token: Token)
    }

    fun setTokenClickListener(tokenClickListener: TokenClickListener) {
        this.tokenClickListener = tokenClickListener
    }

    override fun getItemCount() = if (showManageInfo) dataSet.size + 1 else dataSet.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTokenAccountDetailsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == itemCount - 1 && showManageInfo) {
            holder.binding.addMore.visibility = View.VISIBLE
            holder.binding.content.visibility = View.GONE
            return
        } else {
            holder.binding.addMore.visibility = View.GONE
            holder.binding.content.visibility = View.VISIBLE
        }

        val token = dataSet[position]

        if (isFungible && token.isCCDToken) {
            holder.binding.title.text =
                "${CurrencyUtilImpl.formatGTU(token.totalBalance, true)} CCD"

            Glide.with(context).load(R.drawable.ic_concordium_logo_no_text)
                .into(holder.binding.tokenIcon)
        } else {
            token.tokenMetadata?.let { tokenMetadata ->
                if (tokenMetadata.thumbnail != null && !tokenMetadata.thumbnail.url.isNullOrBlank()) {
                    Glide.with(context)
                        .load(tokenMetadata.thumbnail.url)
                        .placeholder(R.drawable.ic_token_loading_image)
                        .override(iconSize)
                        .fitCenter()
                        .error(R.drawable.ic_token_no_image)
                        .into(holder.binding.tokenIcon)
                } else {
                    holder.binding.tokenIcon.setImageResource(R.drawable.ic_token_no_image)
                }
                if (token.totalBalance != null) {
                    holder.binding.title.text = "${
                        CurrencyUtilImpl.formatGTU(
                            token.totalBalance,
                            false,
                            token.tokenMetadata?.decimals ?: 6
                        )
                    } ${token.symbol}"
                }

            }
        }

        holder.binding.root.setOnClickListener {
            if (!token.isCCDToken || !showManageInfo)
                tokenClickListener?.onRowClick(token)
        }
    }
}
