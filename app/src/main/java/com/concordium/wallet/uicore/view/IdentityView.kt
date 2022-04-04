package com.concordium.wallet.uicore.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.ImageUtil
import kotlinx.android.synthetic.main.view_identity.view.*


class IdentityView : CardView {

    private lateinit var rootLayout: View
    private lateinit var nameTextView: TextView
    private lateinit var expiresTextView: TextView
    private lateinit var iconImageView: ImageView
    private lateinit var statusImageView: ImageView

    private var onItemClickListener: OnItemClickListener? = null

    constructor (context: Context) : super(context) {
        init(null)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun init(attrs: AttributeSet?) {
        val view = View.inflate(context, R.layout.view_identity, this)

        rootLayout = view.root_layout
        nameTextView = view.name_textview
        expiresTextView = view.expires_textview
        iconImageView = view.logo_imageview
        statusImageView = view.status_imageview
    }

    fun setIdentityData(identity: Identity) {
        if (!TextUtils.isEmpty(identity.identityProvider.metadata.icon)) {
            val image = ImageUtil.getImageBitmap(identity.identityProvider.metadata.icon)
            iconImageView.setImageBitmap(image)
        }

        statusImageView.setImageResource(
            when (identity.status) {
                IdentityStatus.PENDING -> R.drawable.ic_pending
                IdentityStatus.DONE -> R.drawable.ic_ok_filled
                IdentityStatus.ERROR -> R.drawable.ic_status_problem
                else -> 0
            }
        )

        foreground = context.getDrawable( if(identity.status == IdentityStatus.DONE || identity.status == IdentityStatus.PENDING) R.drawable.bg_cardview_border else R.drawable.bg_cardview_error_border)
        isEnabled = identity.status != IdentityStatus.PENDING

        nameTextView.text = identity.name

        val identityObject = identity.identityObject
        if(identityObject != null && !TextUtils.isEmpty(identityObject.attributeList.validTo)) {
            val expireDate = DateTimeUtil.convertShortDate(identityObject.attributeList.validTo)
            expiresTextView.text = context.getString(R.string.view_identity_attributes_expiry_data, expireDate)
        } else {
            expiresTextView.text = ""
        }

        // Click
        rootLayout.setOnClickListener {
            onItemClickListener?.onItemClicked(identity)
        }
    }

    //region OnItemClickListener
    //************************************************************

    interface OnItemClickListener {
        fun onItemClicked(item: Identity)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    //endregion
}