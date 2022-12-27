package com.concordium.wallet.ui.walletconnect.proof.of.identity

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.ProofZeroKnowledge
import com.concordium.wallet.databinding.DialogRevealInformationBinding
import com.concordium.wallet.databinding.ItemProofOfIdentityZeroBinding

class ProofOfIdentityZeroProofAdapter(
    private val context: Context,
    var arrayList: Array<ProofZeroKnowledge>
) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val binding: ItemProofOfIdentityZeroBinding

        if (convertView == null) {
            binding =
                ItemProofOfIdentityZeroBinding.inflate(LayoutInflater.from(context), parent, false)
            holder = ViewHolder(binding)
            holder.binding.root.tag = holder
        } else {
            holder = convertView.tag as ProofOfIdentityZeroProofAdapter.ViewHolder
        }

        val zeroProof = arrayList[position]
        holder.binding.content.title.text = zeroProof.name
        holder.binding.content.proof.text = zeroProof.value
        holder.binding.title.text = context.getString(R.string.proof_of_identity_zero_title, zeroProof.title)

        if(!zeroProof.description.isNullOrEmpty()){
            holder.binding.explanation.visibility = View.VISIBLE
            holder.binding.explanation.text = zeroProof.description
        }else{
            holder.binding.explanation.visibility = View.GONE
        }

        if (zeroProof.status == true) {
            loadRevealProofMeet(holder.binding)
        } else {
            loadRevealProofFailed(holder.binding)
        }

        holder.binding.info.setOnClickListener {
            showInfoDialog(
                title = context.getString(R.string.dialog_identity_proof_zero_title),
                content = context.getString(R.string.dialog_identity_proof_zero_content),
                icon = R.drawable.question_mark_circle
            )
        }
        return holder.binding.root
    }

    private fun loadRevealProofMeet(binding: ItemProofOfIdentityZeroBinding) {
        val mark: Drawable? = AppCompatResources.getDrawable(context, R.drawable.ic_ok)
        binding.content.mark.imageTintList =
            AppCompatResources.getColorStateList(context, R.color.text_green)
        binding.header.background =
            ContextCompat.getDrawable(context, R.drawable.rounded_top_blue)
        binding.proofsHolder.background =
            ContextCompat.getDrawable(context, R.drawable.rounded_bottom_transparent)
        binding.subTitle.text = context.getString(R.string.proof_of_identity_meet)
        Glide.with(context).load(mark).fitCenter().into(binding.proofStatus)
        Glide.with(context)
            .load(mark)
            .fitCenter()
            .into(binding.content.mark)
    }

    private fun loadRevealProofFailed(binding: ItemProofOfIdentityZeroBinding) {
        val mark: Drawable? = AppCompatResources.getDrawable(context, R.drawable.ic_close_cross)
        binding.content.mark.imageTintList =
            AppCompatResources.getColorStateList(context, R.color.text_red)
        binding.header.background =
            ContextCompat.getDrawable(context, R.drawable.rounded_top_grey)
        binding.proofsHolder.background = ContextCompat.getDrawable(
            context, R.drawable.rounded_bottom_transparent_grey_border
        )
        binding.subTitle.text = context.getString(R.string.proof_of_identity_not_meet)
        Glide.with(context).load(mark).fitCenter().into(binding.proofStatus)
        Glide.with(context)
            .load(mark)
            .fitCenter()
            .into(binding.content.mark)
    }

    private fun showInfoDialog(title: String, content: String, icon: Int) {
        val bind: DialogRevealInformationBinding =
            DialogRevealInformationBinding.inflate(LayoutInflater.from(context))
        bind.title.text = title
        bind.content.text = content

        Glide.with(context)
            .load(icon)
            .fitCenter()
            .into(bind.info)

        val builder = AlertDialog.Builder(context)
        builder.setView(bind.root)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        bind.link.movementMethod = LinkMovementMethod.getInstance()

        bind.okButton.setOnClickListener {
            dialog.dismiss()
        }
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

    private inner class ViewHolder(val binding: ItemProofOfIdentityZeroBinding)
}