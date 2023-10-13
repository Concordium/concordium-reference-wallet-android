package com.concordium.wallet.ui.passphrase.setup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ItemPassphraseWordPickedBinding
import com.concordium.wallet.ui.passphrase.common.WordsPickedBaseListAdapter
import com.concordium.wallet.ui.passphrase.setup.PassPhraseViewModel.Companion.WORD_COUNT
import com.concordium.wallet.util.UnitConvertUtil

class WordsPickedListAdapter(private val context: Context, private val arrayList: Array<String?>) :
    WordsPickedBaseListAdapter(arrayList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val binding: ItemPassphraseWordPickedBinding

        if (convertView == null) {
            binding =
                ItemPassphraseWordPickedBinding.inflate(LayoutInflater.from(context), parent, false)
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
        rootLayoutParams.height =
            if (position == WORD_COUNT + OFFSET) 0 else UnitConvertUtil.convertDpToPixel(50f)
        holder.binding.root.layoutParams = rootLayoutParams

        arrayList[position]?.let {
            if (it == BLANK) {
                holder.binding.root.visibility = View.GONE
            } else {
                holder.binding.tvTitle.text = it
                holder.binding.tvPosition.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.white
                    )
                )
                holder.binding.tvTitle.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.white
                    )
                )
                holder.binding.rlBorder.elevation = 0f
                when (position) {
                    currentPosition -> {
                        holder.binding.rlBorder.background =
                            AppCompatResources.getDrawable(context, R.drawable.rounded_blue)
                        holder.binding.rlBorder.elevation = 5f
                    }

                    2 -> holder.binding.rlBorder.background =
                        AppCompatResources.getDrawable(context, R.drawable.rounded_top_blue)

                    25 -> holder.binding.rlBorder.background =
                        AppCompatResources.getDrawable(context, R.drawable.rounded_bottom_blue)

                    else -> {
                        if (position - 1 == currentPosition)
                            holder.binding.rlBorder.background =
                                AppCompatResources.getDrawable(context, R.drawable.rectangle_blue)
                        else
                            holder.binding.rlBorder.background = AppCompatResources.getDrawable(
                                context,
                                R.drawable.rectangle_top_blue
                            )
                    }
                }
            }
        } ?: run {
            holder.binding.tvTitle.text = ""
            holder.binding.tvPosition.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.black
                )
            )
            holder.binding.rlBorder.background =
                AppCompatResources.getDrawable(context, R.drawable.rounded_white_blue_border)
            holder.binding.rlBorder.elevation = 0f
            when (position) {
                currentPosition -> {
                    holder.binding.rlBorder.background =
                        AppCompatResources.getDrawable(context, R.drawable.rounded_white)
                    holder.binding.rlBorder.elevation = 5f
                }

                2 -> holder.binding.rlBorder.background =
                    AppCompatResources.getDrawable(context, R.drawable.rounded_top_transparent)

                25 -> holder.binding.rlBorder.background =
                    AppCompatResources.getDrawable(context, R.drawable.rounded_bottom_transparent)

                else -> holder.binding.rlBorder.background =
                    AppCompatResources.getDrawable(context, R.drawable.rectangle_start_end_border)
            }
        }

        holder.binding.root.setOnClickListener {
            wordPickedClickListener?.onClick(position)
        }

        val layoutParams = holder.binding.rlBorder.layoutParams as LinearLayout.LayoutParams
        if (position == currentPosition) {
            layoutParams.setMargins(0, 3, 0, 3)
        } else {
            layoutParams.setMargins(20, 0, 20, 0)
        }
        holder.binding.rlBorder.layoutParams = layoutParams

        return holder.binding.root
    }

    private inner class ViewHolder(val binding: ItemPassphraseWordPickedBinding)
}
