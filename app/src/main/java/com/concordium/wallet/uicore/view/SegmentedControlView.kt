package com.concordium.wallet.uicore.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ViewSegmentedControlBinding

class SegmentedControlView : LinearLayout {
    private val binding =
        ViewSegmentedControlBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var rootLayout: LinearLayout

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
        rootLayout = binding.containerLayout

        if (isInEditMode) {
            addControl("My test 1", null, false)
            addControl("My test 2", null, true)
            addControl("My test 3", null, false)
        }
    }

    fun clearAll() {
        rootLayout.removeAllViews()
    }

    fun addControl(
        title: String,
        clickListener: OnItemClickListener?,
        initiallySelected: Boolean
    ): View {
        val view =
            LayoutInflater.from(context).inflate(R.layout.segmented_text_item, null) as Button
        view.text = title
        val param = LayoutParams(0, LayoutParams.MATCH_PARENT)
        param.weight = 1f
        view.layoutParams = param
        view.isSelected = initiallySelected
        view.setOnClickListener {
            selectItem(it as Button)
            clickListener?.onItemClicked()
        }
        rootLayout.addView(view)
        return view
    }

    fun selectItem(item: Button) {
        for (i in 0 until rootLayout.childCount) {
            val child = rootLayout.getChildAt(i) as Button
            child.isSelected = false
        }
        item.isSelected = true
    }

    //region OnItemClickListener
    //************************************************************

    interface OnItemClickListener {
        fun onItemClicked()
    }

    //endregion
}
