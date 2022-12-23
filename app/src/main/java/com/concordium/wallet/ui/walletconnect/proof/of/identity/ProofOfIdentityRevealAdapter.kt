package com.concordium.wallet.ui.walletconnect.proof.of.identity

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.content.res.AppCompatResources
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.ProofReveal
import com.concordium.wallet.databinding.ItemProofOfIdentityConditionBinding

class ProofOfIdentityRevealAdapter(
    private val context: Context,
    var arrayList: Array<ProofReveal>
) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val binding: ItemProofOfIdentityConditionBinding

        if (convertView == null) {
            binding = ItemProofOfIdentityConditionBinding.inflate(LayoutInflater.from(context), parent, false)
            holder = ViewHolder(binding)
            holder.binding.root.tag = holder
        } else {
            holder = convertView.tag as ProofOfIdentityRevealAdapter.ViewHolder
        }

        val revealProof = arrayList[position]
        holder.binding.title.text = revealProof.name
        holder.binding.proof.text = revealProof.value
        val mark: Drawable?
        if(revealProof.status == true){
            mark = AppCompatResources.getDrawable(context, R.drawable.ic_ok)
            holder.binding.mark.imageTintList = AppCompatResources.getColorStateList(context, R.color.text_green)
        }else{
            mark = AppCompatResources.getDrawable(context, R.drawable.ic_close_cross)
            holder.binding.mark.imageTintList = AppCompatResources.getColorStateList(context, R.color.text_red)
        }
        Glide.with(context)
            .load(mark)
            .fitCenter()
            .into(holder.binding.mark)

        return holder.binding.root
    }


    override fun getCount(): Int {
       return arrayList.size
    }

    override fun getItem(p0: Int): Any? {
        return null
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    private inner class ViewHolder(val binding: ItemProofOfIdentityConditionBinding)
}