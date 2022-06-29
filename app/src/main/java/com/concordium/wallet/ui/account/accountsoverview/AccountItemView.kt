package com.concordium.wallet.ui.account.accountsoverview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.concordium.wallet.R
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ItemAccountBinding

class AccountItemView(context: Context, attrs: AttributeSet?): LinearLayout(context, attrs) {

    private val binding = ItemAccountBinding.inflate(LayoutInflater.from(context), this, true)

    private var accountWithIdentitiy: AccountWithIdentity? = null
    private var hideExpandBar: Boolean = false

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

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
        binding.totalTextview.text = CurrencyUtil.formatGTU(accountWithIdentitiy.account.totalUnshieldedBalance, withGStroke = true)
        binding.balanceAtDisposalTextview.text = CurrencyUtil.formatGTU(accountWithIdentitiy.account.getAtDisposalWithoutStakedOrScheduled(accountWithIdentitiy.account.totalUnshieldedBalance), withGStroke = true)
        binding.accountNameArea.setData(accountWithIdentitiy)

        val accountPending = accountWithIdentitiy.account.transactionStatus == TransactionStatus.COMMITTED || accountWithIdentitiy.account.transactionStatus == TransactionStatus.RECEIVED

        binding.buttonArea.visibility = if(accountPending || accountWithIdentitiy.account.readOnly || hideExpandBar) View.GONE else View.VISIBLE
        binding.rootCardContent.setBackgroundColor(if(accountWithIdentitiy.account.readOnly) resources.getColor(R.color.theme_component_background_disabled, null) else resources.getColor(R.color.theme_white, null))

        this.isEnabled = !accountWithIdentitiy.account.readOnly

    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        if (onItemClickListener != null) {
            binding.accountCardActionSend.setOnClickListener {
                accountWithIdentitiy?.let { it1 -> onItemClickListener.onSendClicked(it1.account) }
            }
            binding.accountCardActionReceive.setOnClickListener {
                accountWithIdentitiy?.let { it1 -> onItemClickListener.onReceiveClicked(it1.account) }
            }
            binding.accountCardActionMore.setOnClickListener {
                accountWithIdentitiy?.let { it1 -> onItemClickListener.onMoreClicked(it1.account) }
            }
            binding.rootCard.setOnClickListener {
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
