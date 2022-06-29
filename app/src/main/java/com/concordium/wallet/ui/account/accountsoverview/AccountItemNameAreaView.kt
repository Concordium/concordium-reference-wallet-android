package com.concordium.wallet.ui.account.accountsoverview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.concordium.wallet.R
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.databinding.ItemAccountNameAreaBinding

class AccountItemNameAreaView(context: Context, attrs: AttributeSet?): LinearLayout(context, attrs) {
    private val binding = ItemAccountNameAreaBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun setData(accountWithIdentitiy: AccountWithIdentity) {
        binding.statusImageview.setImageResource(
            when (accountWithIdentitiy.account.transactionStatus) {
                TransactionStatus.COMMITTED -> R.drawable.ic_pending
                TransactionStatus.RECEIVED -> R.drawable.ic_pending
                TransactionStatus.ABSENT -> R.drawable.ic_status_problem
                else -> 0
            }
        )

        binding.statusImageview.visibility = if (accountWithIdentitiy.account.transactionStatus == TransactionStatus.FINALIZED) View.GONE else View.VISIBLE
        binding.nameTextview.text = accountWithIdentitiy.account.getAccountName()

        binding.statusIcon.visibility = View.VISIBLE
        binding.statusText.visibility = View.VISIBLE
        if (accountWithIdentitiy.account.isBaking()) {
            binding.statusIcon.setImageResource(R.drawable.ic_baking)
            binding.statusText.text = context.getString(R.string.view_account_baking)
        } else if (accountWithIdentitiy.account.isDelegating()) {
            binding.statusIcon.setImageResource(R.drawable.ic_delegating)
            binding.statusText.text = context.getString(R.string.view_account_delegating)
        } else if (accountWithIdentitiy.account.readOnly) {
            binding.statusIcon.setImageResource(R.drawable.ic_read_only)
            binding.statusText.text = context.getString(R.string.view_account_read_only)
        } else {
            binding.statusIcon.visibility = View.GONE
            if (accountWithIdentitiy.account.isInitial())
                binding.statusText.text = context.getString(R.string.view_account_initial)
            else
                binding.statusText.visibility = View.GONE
        }

        binding.accountIdentityName.text = context.getString(R.string.view_account_name_container,accountWithIdentitiy.identity.name)

        // Fix for a weird error where name is truncated wrong and at random
        // This fixes it. We expand if text is truncated, else we wrap.
        post {
            val ellipsisCount = binding.nameTextview.layout.getEllipsisCount(0)
            if (ellipsisCount == 0) {
                binding.nameTextview.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            }
            else {
                binding.nameTextview.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT,1f)
            }
        }
    }
}
