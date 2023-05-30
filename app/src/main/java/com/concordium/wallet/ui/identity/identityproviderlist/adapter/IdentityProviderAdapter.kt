package com.concordium.wallet.ui.identity.identityproviderlist.adapter

import android.content.Context
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.databinding.ItemIdentityProviderBinding
import com.concordium.wallet.databinding.ItemIdentityProviderHeaderBinding
import com.concordium.wallet.util.ImageUtil.getImageBitmap

class IdentityProviderAdapter(
    private val context: Context,
    private val identityName: String,
    private val showProgressLine: Boolean,
    private var data: List<AdapterItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null

    inner class ItemViewHolder(val binding: ItemIdentityProviderBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class HeaderViewHolder(val binding: ItemIdentityProviderHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = data.size

    override fun getItemViewType(position: Int): Int {
        val item = data[position]
        return item.getItemType().id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ItemType.Header.id -> {
                val binding = ItemIdentityProviderHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                binding.progressLine.visibility = if (showProgressLine) View.VISIBLE else View.GONE
                HeaderViewHolder(binding)
            }

            ItemType.Item.id -> {
                val binding = ItemIdentityProviderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ItemViewHolder(binding)
            }

            else -> {
                throw Exception("Invalid item view type")
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            with(holder) {
                binding.infoTextview.text = Html.fromHtml(
                    context.getString(
                        R.string.identity_create_identity_provider_info,
                        identityName
                    )
                )
                binding.infoTextview.movementMethod = LinkMovementMethod.getInstance()
                binding.infoTextview.linksClickable = true
            }
        } else if (holder is ItemViewHolder) {
            with(holder) {
                val item = data[position] as IdentityProviderItem
                val identityProvider = item.identityProvider
                if (!TextUtils.isEmpty(identityProvider.metadata.icon)) {
                    val image = getImageBitmap(identityProvider.metadata.icon)
                    binding.logoImageview.setImageBitmap(image)
                }
                val description = identityProvider.ipInfo.ipDescription
                binding.headerTextview.text = description.name

                // Click
                if (onItemClickListener != null) {
                    binding.mainAreaView.setOnClickListener {
                        onItemClickListener?.onItemClicked(identityProvider)
                    }
                    binding.secondaryAreaView.setOnClickListener {
                        onItemClickListener?.onItemActionClicked(identityProvider)
                    }
                }
            }
        }
    }

    fun setData(data: List<AdapterItem>) {
        this.data = data
    }

    //region OnItemClickListener
    //************************************************************

    interface OnItemClickListener {
        fun onItemClicked(item: IdentityProvider)
        fun onItemActionClicked(item: IdentityProvider)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    //endregion
}