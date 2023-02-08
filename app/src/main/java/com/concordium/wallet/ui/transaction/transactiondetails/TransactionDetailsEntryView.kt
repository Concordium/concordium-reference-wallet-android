package com.concordium.wallet.ui.transaction.transactiondetails

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.getResourceIdOrThrow
import com.concordium.wallet.R
import com.concordium.wallet.uicore.Formatter
import kotlinx.android.synthetic.main.view_transaction_details_entry.view.*

class TransactionDetailsEntryView : ConstraintLayout {

    private var fullValue: String? = null

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
        View.inflate(context, R.layout.view_transaction_details_entry, this)

        if (attrs != null) {
            val ta =
                context.obtainStyledAttributes(attrs, R.styleable.TransactionDetailsEntryView, 0, 0)
            try {
                title_textview.setText(ta.getResourceIdOrThrow(R.styleable.TransactionDetailsEntryView_entry_title))
            } finally {
                ta.recycle()
            }
        }

        copy_imageview.visibility = View.GONE
    }

    fun setTitle(title: String) {
        title_textview.text = title
    }

    fun setValue(value: String, formatAsFirstEight: Boolean = false) {
        fullValue = value
        value_textview.text = if(formatAsFirstEight) Formatter.formatAsFirstEight(value) else value
    }

    fun enableCopy(onCopyClickListener: OnCopyClickListener) {
        listener = onCopyClickListener
        copy_imageview.visibility = View.VISIBLE
        copy_imageview.setOnClickListener {
            fullValue?.let { value ->
                val title = title_textview.text.toString()
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