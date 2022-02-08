package com.concordium.wallet.ui.account.accountsoverview

import android.content.Context
import android.text.Layout
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.concordium.wallet.R
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.AccountWithIdentity
import kotlinx.android.synthetic.main.item_account_name_area.view.*


class AccountItemNameAreaView(context: Context, attrs: AttributeSet?): LinearLayout(context, attrs) {

    init {
        inflate(context, R.layout.item_account_name_area, this)
        setLayoutParams(LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));


    }

    fun setData(accountWithIdentitiy: AccountWithIdentity){

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
        initial_textview.visibility = if(accountWithIdentitiy.account.isInitial()) View.VISIBLE else View.GONE

        status_read_only.visibility = if(accountWithIdentitiy.account.readOnly) View.VISIBLE else View.GONE
        status_baker.visibility = if(accountWithIdentitiy.account.isBaker()) View.VISIBLE else View.GONE

        account_identity_name.text = accountWithIdentitiy.identity.name

        //Fix for a weird error where name is truncated wrong and at random
        //This fixes it. We expand if text is truncated, else we wrap.
        post {
            val layout: Layout = name_textview.getLayout()
            if (layout != null) {
                val ellipsisCount = layout.getEllipsisCount(0)
                if(ellipsisCount == 0){
                    name_textview.setLayoutParams(LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                }
                else{
                    name_textview.setLayoutParams(LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,1f))
                }
            }
        }

    }


}