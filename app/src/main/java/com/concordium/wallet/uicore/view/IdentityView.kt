package com.concordium.wallet.uicore.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ViewIdentityBinding
import com.concordium.wallet.util.ImageUtil
import com.concordium.wallet.util.UnitConvertUtil.convertDpToPixel

class IdentityView : CardView {
    private val binding = ViewIdentityBinding.inflate(LayoutInflater.from(context), this, true)
    private var onItemClickListener: OnItemClickListener? = null
    private var onChangeNameClickListener: OnChangeNameClickListener? = null

    constructor (context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun init(attrs: AttributeSet?) {
    }

    fun enableChangeNameOption(identity: Identity) {
        binding.edit.visibility = View.VISIBLE
        binding.edit.setOnClickListener {
            onChangeNameClickListener?.onChangeNameClicked(identity)
        }
    }

    fun setIdentityData(identity: Identity) {
        binding.identityProviderName.text = identity.identityProvider.ipInfo.ipDescription.name

        if (!TextUtils.isEmpty(identity.identityProvider.metadata.icon)) {
            val image = ImageUtil.getImageBitmap(identity.identityProvider.metadata.icon)
            val layoutParams = binding.identityProviderLogo.layoutParams
            layoutParams.height = convertDpToPixel(50f)
            if (image.width > image.height) {
                val aspectRatio = image.width.toFloat() / image.height.toFloat()
                layoutParams.width = ((image.height.toFloat() * aspectRatio) - layoutParams.height.toFloat()).toInt()
            } else if (image.width < image.height) {
                val aspectRatio = image.height.toFloat() / image.width.toFloat()
                layoutParams.width = ((image.height.toFloat() * aspectRatio) - layoutParams.height.toFloat()).toInt()
            } else {
                layoutParams.width = layoutParams.height
            }
            binding.identityProviderLogo.layoutParams = layoutParams
            binding.identityProviderLogo.setImageBitmap(image)
        }

        binding.statusIcon.setImageResource(
            when (identity.status) {
                IdentityStatus.PENDING -> R.drawable.ic_pending_white
                IdentityStatus.DONE -> R.drawable.ic_ok_filled
                IdentityStatus.ERROR -> R.drawable.ic_status_problem
                else -> 0
            }
        )

        val drawableResource = if (identity.status == IdentityStatus.DONE || identity.status == IdentityStatus.PENDING) R.drawable.bg_cardview_border else R.drawable.bg_cardview_error_border
        foreground = AppCompatResources.getDrawable(context, drawableResource)
        isEnabled = identity.status != IdentityStatus.PENDING

        binding.identityName.text = identity.name

        binding.rootLayout.setOnClickListener {
            onItemClickListener?.onItemClicked(identity)
        }
    }

    interface OnItemClickListener {
        fun onItemClicked(item: Identity)
    }

    interface OnChangeNameClickListener {
        fun onChangeNameClicked(item: Identity)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    fun setOnChangeNameClickListener(onChangeNameClickListener: OnChangeNameClickListener) {
        this.onChangeNameClickListener = onChangeNameClickListener
    }
}
