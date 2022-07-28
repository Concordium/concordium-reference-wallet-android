package com.concordium.wallet.ui.passphrase.recover

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ListItemWordsPickedRecoverBinding
import com.concordium.wallet.ui.passphrase.common.WordsPickedBaseListAdapter
import com.concordium.wallet.ui.passphrase.setup.PassPhraseViewModel
import com.concordium.wallet.util.UnitConvertUtil

class WordsPickedRecoverListAdapter(private val context: Context, private val arrayList: Array<String?>) : WordsPickedBaseListAdapter(arrayList) {
    private var onTextChangeListener: OnTextChangeListener? = null

    fun interface OnTextChangeListener {
        fun onTextChange(text: String)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val binding: ListItemWordsPickedRecoverBinding

        if (convertView == null) {
            binding = ListItemWordsPickedRecoverBinding.inflate(LayoutInflater.from(context), parent, false)
            holder = ViewHolder(binding)
            val layoutParams = holder.binding.tvPosition.layoutParams
            layoutParams.width = widestPositionText
            holder.binding.tvPosition.layoutParams = layoutParams
            holder.binding.root.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        holder.binding.tvPosition.text = String.format("%d.", (position + 1) - OFFSET)
        holder.binding.root.visibility = View.VISIBLE

        val rootLayoutParams = holder.binding.root.layoutParams
        rootLayoutParams.height = if (position == PassPhraseViewModel.WORD_COUNT + OFFSET) 0 else  UnitConvertUtil.convertDpToPixel(50f)
        holder.binding.root.layoutParams = rootLayoutParams

        arrayList[position]?.let {
            if (it == BLANK) {
                holder.binding.root.visibility = View.GONE
            } else {
                holder.binding.etTitle.setText(it)
                holder.binding.tvTitle.text = it
                holder.binding.tvPosition.setTextColor(ContextCompat.getColor(context, R.color.theme_white))
                holder.binding.etTitle.setTextColor(ContextCompat.getColor(context, R.color.theme_white))
                holder.binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.theme_white))
                when (position) {
                    currentPosition -> holder.binding.rlBorder.background = AppCompatResources.getDrawable(context, R.drawable.rounded_blue_gradient_border)
                    2 -> holder.binding.rlBorder.background = AppCompatResources.getDrawable(context, R.drawable.rounded_top_blue)
                    25 -> holder.binding.rlBorder.background = AppCompatResources.getDrawable(context, R.drawable.rounded_bottom_blue)
                    else -> {
                        if (position - 1 == currentPosition)
                            holder.binding.rlBorder.background = AppCompatResources.getDrawable(context, R.drawable.rectangle_blue)
                        else
                            holder.binding.rlBorder.background = AppCompatResources.getDrawable(context, R.drawable.rectangle_top_blue)
                    }
                }
            }
        } ?: run {
            holder.binding.etTitle.setText("")
            holder.binding.tvTitle.text = ""
            holder.binding.tvPosition.setTextColor(ContextCompat.getColor(context, R.color.theme_black))
            holder.binding.etTitle.setTextColor(ContextCompat.getColor(context, R.color.theme_black))
            holder.binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.theme_black))
            holder.binding.rlBorder.background = AppCompatResources.getDrawable(context, R.drawable.rounded_white_blue_border)
            when (position) {
                currentPosition -> holder.binding.rlBorder.background = AppCompatResources.getDrawable(context, R.drawable.rounded_white_gradient_border)
                2 -> holder.binding.rlBorder.background = AppCompatResources.getDrawable(context, R.drawable.rounded_top_transparent)
                25 -> holder.binding.rlBorder.background = AppCompatResources.getDrawable(context, R.drawable.rounded_bottom_transparent)
                else -> holder.binding.rlBorder.background = AppCompatResources.getDrawable(context, R.drawable.rectangle_start_end_border)
            }
        }

        holder.binding.rlInput.setOnClickListener {
            wordPickedClickListener?.onClick(position)
        }

        holder.binding.etTitle.doOnTextChanged { text, _, _, _ ->
            onTextChangeListener?.onTextChange(text.toString())
        }

        val marginTopBottom = 0
        val layoutParams = holder.binding.rlBorder.layoutParams as LinearLayout.LayoutParams

        if (position == currentPosition) {
            layoutParams.setMargins(0, marginTopBottom, 0, marginTopBottom)
        } else {
            layoutParams.setMargins(10, marginTopBottom, 10, marginTopBottom)
        }
        holder.binding.rlBorder.layoutParams = layoutParams

        holder.binding.etTitle.showSoftInputOnFocus = true

        if (position == currentPosition) {
            holder.binding.etTitle.visibility = View.VISIBLE
            holder.binding.tvTitle.visibility = View.GONE
            holder.binding.etTitle.requestFocus()
            holder.binding.etTitle.setSelection(holder.binding.etTitle.text.length)
        } else {
            holder.binding.etTitle.visibility = View.GONE
            holder.binding.tvTitle.visibility = View.VISIBLE
        }

        return holder.binding.root
    }

    fun setOnTextChangeListener(onTextChangeListener: OnTextChangeListener) {
        this.onTextChangeListener = onTextChangeListener
    }

    private inner class ViewHolder(val binding: ListItemWordsPickedRecoverBinding)
}
