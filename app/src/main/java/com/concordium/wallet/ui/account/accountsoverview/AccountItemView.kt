package com.concordium.wallet.ui.account.accountsoverview

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.concordium.wallet.R
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.util.CurrencyUtil
import kotlinx.android.synthetic.main.item_account.view.*


class AccountItemView(context: Context, attrs: AttributeSet?): LinearLayout(context, attrs) {

    private var accountWithIdentitiy: AccountWithIdentity? = null
    private var hideExpandBar: Boolean = false

    init {
        inflate(context, R.layout.item_account, this)
        setLayoutParams(LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.AccountItemView, 0, 0)
            try {
                hideExpandBar = ta.getBoolean(R.styleable.AccountItemView_hide_expand_bar, false)
            } finally {
                ta.recycle()
            }
        }
    }


    fun setAccount(accountWithIdentitiy: AccountWithIdentity) {
        this.accountWithIdentitiy = accountWithIdentitiy
        total_textview.text = CurrencyUtil.formatGTU(accountWithIdentitiy.account.totalUnshieldedBalance, withGStroke = true)
        balance_at_disposal_textview.text = CurrencyUtil.formatGTU(accountWithIdentitiy.account.totalUnshieldedBalance - accountWithIdentitiy.account.getAtDisposalSubstraction(), withGStroke = true)
        account_name_area.setData(accountWithIdentitiy)

        var accountPending = if(accountWithIdentitiy.account.transactionStatus == TransactionStatus.COMMITTED || accountWithIdentitiy.account.transactionStatus == TransactionStatus.RECEIVED) true else false

        button_area.visibility = if(accountPending || accountWithIdentitiy.account.readOnly || hideExpandBar) View.GONE else View.VISIBLE
        root_card_content.setBackgroundColor(if(accountWithIdentitiy.account.readOnly) resources.getColor(R.color.theme_component_background_disabled, null) else resources.getColor(R.color.theme_white, null))

        this.isEnabled = !accountWithIdentitiy.account.readOnly

    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        if (onItemClickListener != null) {
            account_card_action_send.setOnClickListener {
                accountWithIdentitiy?.let { it1 -> onItemClickListener.onSendClicked(it1.account) }
            }
            account_card_action_receive.setOnClickListener {
                accountWithIdentitiy?.let { it1 -> onItemClickListener.onReceiveClicked(it1.account) }
            }
            account_card_action_more.setOnClickListener {
                accountWithIdentitiy?.let { it1 -> onItemClickListener.onMoreClicked(it1.account) }
            }
            root_card.setOnClickListener {
                accountWithIdentitiy?.let { it1 -> onItemClickListener.onMoreClicked(it1.account) }
            }
        }
    }


    interface OnItemClickListener {
        fun onMoreClicked(item: Account)
        fun onReceiveClicked(item: Account)
        fun onSendClicked(item: Account)
    }
}