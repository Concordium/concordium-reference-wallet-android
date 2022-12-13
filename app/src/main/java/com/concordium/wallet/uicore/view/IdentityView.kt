package com.concordium.wallet.uicore.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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

        when (identity.status) {
            IdentityStatus.PENDING -> {
                binding.statusIcon.setImageResource(R.drawable.ic_pending_white)
                binding.top.background = ContextCompat.getDrawable(context, R.drawable.rounded_top_grey)
                binding.statusText.text = context.getString(R.string.identity_pending_verification_with)
                binding.rosette.visibility = View.GONE
            }
            IdentityStatus.DONE -> {
                binding.statusIcon.setImageResource(R.drawable.ic_ok_filled)
                binding.top.background = ContextCompat.getDrawable(context, R.drawable.rounded_top_dark_blue)
                binding.statusText.text = context.getString(R.string.identity_verified_by)
                binding.rosette.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_identity_corner_done))
            }
            IdentityStatus.ERROR -> {
                binding.statusIcon.setImageResource(R.drawable.ic_status_problem)
                binding.top.background = ContextCompat.getDrawable(context, R.drawable.rounded_top_red)
                binding.statusText.text = context.getString(R.string.identity_rejected_by)
                binding.rosette.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_identity_corner_rejected))
            }
        }

        val drawableResource = when (identity.status) {
            IdentityStatus.DONE -> R.drawable.bg_cardview_border_dark_blue
            IdentityStatus.PENDING -> R.drawable.bg_cardview_border_disabled
            IdentityStatus.ERROR -> R.drawable.bg_cardview_error_border
            else -> 0
        }
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
