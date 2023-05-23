package com.concordium.wallet.ui.account.accountsoverview

import android.annotation.SuppressLint
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

class AccountItemView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val binding = ItemAccountBinding.inflate(LayoutInflater.from(context), this, true)

    private var accountWithIdentity: AccountWithIdentity? = null
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

    fun setAccount(account: Account) {
        binding.totalTextview.text =
            CurrencyUtil.formatGTU(account.totalUnshieldedBalance, withGStroke = true)
        binding.balanceAtDisposalTextview.text = CurrencyUtil.formatGTU(
            account.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance),
            withGStroke = true
        )

        val accountPending =
            account.transactionStatus == TransactionStatus.COMMITTED || account.transactionStatus == TransactionStatus.RECEIVED

        binding.buttonArea.visibility =
            if (accountPending || account.readOnly || hideExpandBar) View.GONE else View.VISIBLE
        binding.rootCardContent.setBackgroundColor(
            if (account.readOnly) resources.getColor(
                R.color.theme_component_background_disabled,
                null
            ) else resources.getColor(R.color.theme_white, null)
        )

        this.isEnabled = !account.readOnly
    }

    fun setAccount(accountWithIdentity: AccountWithIdentity) {
        this.accountWithIdentity = accountWithIdentity
        binding.totalTextview.text = CurrencyUtil.formatGTU(
            accountWithIdentity.account.totalUnshieldedBalance,
            withGStroke = true
        )
        binding.balanceAtDisposalTextview.text = CurrencyUtil.formatGTU(
            accountWithIdentity.account.getAtDisposalWithoutStakedOrScheduled(accountWithIdentity.account.totalUnshieldedBalance),
            withGStroke = true
        )
        binding.accountNameArea.setData(accountWithIdentity)

        val accountPending =
            accountWithIdentity.account.transactionStatus == TransactionStatus.COMMITTED || accountWithIdentity.account.transactionStatus == TransactionStatus.RECEIVED

        binding.buttonArea.visibility =
            if (accountPending || accountWithIdentity.account.readOnly || hideExpandBar) View.GONE else View.VISIBLE
        binding.rootCardContent.setBackgroundColor(
            if (accountWithIdentity.account.readOnly) resources.getColor(
                R.color.theme_component_background_disabled,
                null
            ) else resources.getColor(R.color.theme_white, null)
        )

        this.isEnabled = !accountWithIdentity.account.readOnly
    }

    @SuppressLint("SetTextI18n")
    fun setDefault(identityName: String, accountName: String) {
        binding.accountNameArea.setDefault(identityName, accountName)
        binding.totalTextview.text = "123.45"
        binding.balanceAtDisposalTextview.text = "123.45"
        binding.buttonArea.visibility = View.GONE
        val layoutParams = binding.rootCard.layoutParams as MarginLayoutParams
        layoutParams.setMargins(layoutParams.leftMargin, 0, layoutParams.rightMargin, 0)
        binding.rootCard.layoutParams = layoutParams
        isEnabled = false
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        if (onItemClickListener != null) {
            binding.accountCardActionSend.setOnClickListener {
                accountWithIdentity?.let { it1 -> onItemClickListener.onSendClicked(it1.account) }
            }
            binding.accountCardActionReceive.setOnClickListener {
                accountWithIdentity?.let { it1 -> onItemClickListener.onReceiveClicked(it1.account) }
            }
            binding.accountCardActionEarn.setOnClickListener {
                accountWithIdentity?.let { it1 -> onItemClickListener.onEarnClicked(it1.account) }
            }
            binding.accountCardActionMore.setOnClickListener {
                accountWithIdentity?.let { it1 -> onItemClickListener.onMoreClicked(it1.account) }
            }
            binding.rootCard.setOnClickListener {
                accountWithIdentity?.let { it1 -> onItemClickListener.onMoreClicked(it1.account) }
            }
        }
    }

    interface OnItemClickListener {
        fun onEarnClicked(item: Account)
        fun onMoreClicked(item: Account)
        fun onReceiveClicked(item: Account)
        fun onSendClicked(item: Account)
    }
}
