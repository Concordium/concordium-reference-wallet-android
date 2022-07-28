package com.concordium.wallet.ui.passphrase.common

import android.graphics.Paint
import android.graphics.Typeface
import android.widget.BaseAdapter
import com.concordium.wallet.util.UnitConvertUtil

abstract class WordsPickedBaseListAdapter(private val arrayList: Array<String?>) : BaseAdapter() {
    companion object {
        const val BLANK = "BLANK_ITEM"
        const val OFFSET = 2
    }

    protected var wordPickedClickListener: WordPickedClickListener? = null
    protected val widestPositionText = widestPositionText()
    var currentPosition = OFFSET

    fun interface WordPickedClickListener {
        fun onClick(position: Int)
    }

    override fun getCount(): Int {
        return arrayList.size
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @JvmName("setWordPickedClickListener1")
    fun setWordPickedClickListener(wordPickedClickListener: WordPickedClickListener) {
        this.wordPickedClickListener = wordPickedClickListener
    }

    private fun widestPositionText(): Int {
        val paint = Paint()
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        return UnitConvertUtil.convertDpToPixel(paint.measureText("24. "))
    }
}
