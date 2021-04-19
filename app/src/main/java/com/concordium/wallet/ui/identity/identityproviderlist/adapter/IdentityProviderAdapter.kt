package com.concordium.wallet.ui.identity.identityproviderlist.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.util.ImageUtil.getImageBitmap
import kotlinx.android.synthetic.main.item_identity_provider.view.*
import kotlinx.android.synthetic.main.item_identity_provider.view.header_textview
import kotlinx.android.synthetic.main.item_identity_provider.view.root_layout
import kotlinx.android.synthetic.main.item_identity_provider_header.view.*

class IdentityProviderAdapter(
    private val context: Context,
    private val identityName: String,
    private var data: List<AdapterItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null

    class HeaderViewHolder(val view: View, val context: Context) : RecyclerView.ViewHolder(view) {
        private val infoTextView: TextView = view.info_textview

        fun bind(identityName: String) {
            infoTextView.text = context.getString(R.string.identity_create_identity_provider_info, identityName)
        }
    }

    class ItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val rootLayout: View = view.root_layout
        private val mainAreaView: View = view.main_area_view
        private val secondaryAreaView: View = view.secondary_area_view
        private val imageView: ImageView = view.logo_imageview
        private val nameTextView: TextView = view.header_textview
        private val privacyPolicyTextView = view.privacy_policy_textview

        fun bind(item: IdentityProviderItem, onItemClickListener: OnItemClickListener?) {
            val identityProvider = item.identityProvider
            if (!TextUtils.isEmpty(identityProvider.metadata.icon)) {
                val image = getImageBitmap(identityProvider.metadata.icon)
                imageView.setImageBitmap(image)
            }
            val description = identityProvider.ipInfo.ipDescription
            nameTextView.text = description.name

            // Click
            if (onItemClickListener != null) {
                mainAreaView.setOnClickListener {
                    onItemClickListener.onItemClicked(identityProvider)
                }
                secondaryAreaView.setOnClickListener {
                    onItemClickListener.onItemActionClicked(identityProvider)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        val item = data[position]
        return item.getItemType().id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ItemType.Header.id -> {
                HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_identity_provider_header, parent, false), context)
            }
            ItemType.Item.id -> {
                ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_identity_provider, parent, false))
            }
            else -> {
                throw Exception("Invalid item view type")
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data.get(position)
        (holder as? HeaderViewHolder)?.bind(identityName)
        (holder as? ItemViewHolder)?.bind(item as IdentityProviderItem, onItemClickListener)
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