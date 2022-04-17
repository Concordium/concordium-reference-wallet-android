package com.concordium.wallet.ui.account.accountsoverview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.concordium.wallet.R
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.AccountWithIdentity
import kotlinx.android.synthetic.main.item_account_name_area.view.*

class AccountItemNameAreaView(context: Context, attrs: AttributeSet?): LinearLayout(context, attrs) {

    init {
        inflate(context, R.layout.item_account_name_area, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun setData(accountWithIdentitiy: AccountWithIdentity) {
        status_imageview.setImageResource(
            when (accountWithIdentitiy.account.transactionStatus) {
                TransactionStatus.COMMITTED -> R.drawable.ic_pending
                TransactionStatus.RECEIVED -> R.drawable.ic_pending
                TransactionStatus.ABSENT -> R.drawable.ic_status_problem
                else -> 0
            }
        )

        status_imageview.visibility = if (accountWithIdentitiy.account.transactionStatus == TransactionStatus.FINALIZED) View.GONE else View.VISIBLE
        name_textview.text = accountWithIdentitiy.account.getAccountName()

        if (accountWithIdentitiy.account.isBaking()) {
            status_icon.setImageResource(R.drawable.ic_baking)
            status_text.text = context.getString(R.string.view_account_baking)
        } else if (accountWithIdentitiy.account.isDelegating()) {
            status_icon.setImageResource(R.drawable.ic_delegating)
            status_text.text = context.getString(R.string.view_account_delegating)
        } else if (accountWithIdentitiy.account.readOnly) {
            status_icon.setImageResource(R.drawable.ic_read_only)
            status_text.text = context.getString(R.string.view_account_read_only)
        } else {
            status_icon.visibility = View.GONE
            if (accountWithIdentitiy.account.isInitial())
                status_text.text = context.getString(R.string.view_account_initial)
            else
                status_text.visibility = View.GONE
        }

        account_identity_name.text = context.getString(R.string.view_account_name_container,accountWithIdentitiy.identity.name)

        // Fix for a weird error where name is truncated wrong and at random
        // This fixes it. We expand if text is truncated, else we wrap.
        post {
            val ellipsisCount = name_textview.layout.getEllipsisCount(0)
            if (ellipsisCount == 0) {
                name_textview.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            }
            else {
                name_textview.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT,1f)
            }
        }
    }
}
