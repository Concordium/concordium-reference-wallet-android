package com.concordium.wallet.ui.transaction.transactiondetails

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.getResourceIdOrThrow
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ViewTransactionDetailsEntryBinding
import com.concordium.wallet.uicore.Formatter

class TransactionDetailsEntryView : ConstraintLayout {
    private val binding = ViewTransactionDetailsEntryBinding.inflate(LayoutInflater.from(context), this, true)

    private var fullValue: String? = null

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
        if (attrs != null) {
            val ta =
                context.obtainStyledAttributes(attrs, R.styleable.TransactionDetailsEntryView, 0, 0)
            try {
                binding.titleTextview.setText(ta.getResourceIdOrThrow(R.styleable.TransactionDetailsEntryView_entry_title))
            } finally {
                ta.recycle()
            }
        }
        binding.copyImageview.visibility = View.GONE
    }

    fun setTitle(title: String) {
        binding.titleTextview.text = title
    }

    fun setValue(value: String, formatAsFirstEight: Boolean = false) {
        fullValue = value
        binding.valueTextview.text = if(formatAsFirstEight) Formatter.formatAsFirstEight(value) else value
    }

    fun enableCopy(onCopyClickListener: OnCopyClickListener) {
        listener = onCopyClickListener
        binding.copyImageview.visibility = View.VISIBLE
        binding.copyImageview.setOnClickListener {
            fullValue?.let { value ->
                val title = binding.titleTextview.text.toString()
                listener?.onCopyClicked(title, value)
            }
        }
    }

    //region Listener

    interface OnCopyClickListener {
        fun onCopyClicked(title: String, value: String)
    }

    private var listener: OnCopyClickListener? = null

    //endregion
}
