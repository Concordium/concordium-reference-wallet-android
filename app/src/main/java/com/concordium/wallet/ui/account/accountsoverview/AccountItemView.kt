package com.concordium.wallet.ui.account.accountsoverview

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.concordium.wallet.R
import com.concordium.wallet.data.model.ShieldedAccountEncryptionStatus
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.util.Log
import kotlinx.android.synthetic.main.item_account.view.*


class AccountItemView(context: Context, attrs: AttributeSet?): LinearLayout(context, attrs) {


    private var accountWithIdentitiy: AccountWithIdentity? = null
    private var expanded: Boolean = false

    init {
        inflate(context, R.layout.item_account, this)
        setLayoutParams(LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.AccountItemView, 0, 0)
            try {
                hideExpandedBar(ta.getBoolean(R.styleable.AccountItemView_hide_expand_bar, false))
                setExpanded(ta.getBoolean(R.styleable.AccountItemView_expanded, false))
            } finally {
                ta.recycle()
            }
        }

        updateExpansionUI(true)

        account_identity.setOnClickListener(object : OnClickListener {
            override fun onClick(v: View?) {
                toggleExpansion()
            }
        })

        structure_unfolder.setOnClickListener(object : OnClickListener {
            override fun onClick(v: View?) {
                toggleExpansion()
            }
        })
    }

    private fun hideViewAnimated(view: View, instant: Boolean){
            val va = ValueAnimator.ofFloat(context.resources.getDimension(R.dimen.balance_collapsable_item_height), 0f)
            va.duration = if(instant) 0 else context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            va.addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                view.getLayoutParams().height = value.toInt()
                view.requestLayout()
            }
            va.start()
    }

    private fun showViewAnimated(view: View, instant: Boolean){

        val va = ValueAnimator.ofFloat(0f, context.resources.getDimension(R.dimen.balance_collapsable_item_height))
        va.duration = if(instant) 0 else context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        va.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            view.getLayoutParams().height = value.toInt()
            view.requestLayout()
        }
        va.start()
    }


    fun setExpanded(expand:Boolean){
        expanded = expand
        updateExpansionUI(true)
    }

    fun hideExpandedBar(hide:Boolean){
        structure_unfolder.visibility = if(hide) View.GONE else View.VISIBLE
    }

    fun setAccount(accountWithIdentitiy: AccountWithIdentity) {
        this.accountWithIdentitiy = accountWithIdentitiy


        total_textview.text = CurrencyUtil.formatGTU(accountWithIdentitiy.account.totalBalance, withGStroke = true)

        balance_total_textview.text = CurrencyUtil.formatGTU(accountWithIdentitiy.account.totalUnshieldedBalance, withGStroke = true)
        shielded_total_textview.text = CurrencyUtil.formatGTU(accountWithIdentitiy.account.totalShieldedBalance, withGStroke = true)
        shielded_total_textview.visibility = if(accountWithIdentitiy.account.encryptedBalanceStatus == ShieldedAccountEncryptionStatus.DECRYPTED || accountWithIdentitiy.account.encryptedBalanceStatus == ShieldedAccountEncryptionStatus.PARTIALLYDECRYPTED) View.VISIBLE else View.GONE
        shielded_lock_plus.visibility = shielded_total_textview.visibility
        shielded_lock_container.visibility = if(accountWithIdentitiy.account.encryptedBalanceStatus != ShieldedAccountEncryptionStatus.DECRYPTED) View.VISIBLE else View.GONE
        shielded_total_lock_container.visibility = shielded_lock_container.visibility

        balance_staked_textview.text = CurrencyUtil.formatGTU(accountWithIdentitiy.account.totalStaked, withGStroke = true)
        balance_at_disposal_textview.text = CurrencyUtil.formatGTU(accountWithIdentitiy.account.totalBalance - accountWithIdentitiy.account.getAtDisposalSubstraction() - accountWithIdentitiy.account.totalShieldedBalance, withGStroke = true)

        account_identity_name_area.text = accountWithIdentitiy.identity.name


        root_content.alpha = if(accountWithIdentitiy.account.readOnly) 0.4f else 1f

        account_name_area.setData(accountWithIdentitiy)

    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        if (onItemClickListener != null) {
            regular_balance_layout.setOnClickListener {
                accountWithIdentitiy?.let { it1 -> onItemClickListener.onRegularBalanceClicked(it1.account) }
            }
            shielded_balance_layout.setOnClickListener {
                accountWithIdentitiy?.let { it1 -> onItemClickListener.onShieldedBalanceClicked(it1.account) }
            }
        }
    }

    fun toggleExpansion(){
        expanded = !expanded
        updateExpansionUI(false)
    }

    private fun updateExpansionUI(instant:Boolean){

        structure_unfolder.setImageDrawable(resources.getDrawable(if(expanded) R.drawable.ic_unfold else R.drawable.ic_fold, null))


        if(expanded){
            showViewAnimated(account_identity_name_area, instant)
            showViewAnimated(account_amount_title_area, instant)
            showViewAnimated(balance_total_at_disposal_container, instant)
            showViewAnimated(balance_total_staked_container, instant)
            //name_textview.isSingleLine = false
        }
        else{
            hideViewAnimated(account_identity_name_area, instant)
            hideViewAnimated(account_amount_title_area, instant)
            hideViewAnimated(balance_total_at_disposal_container, instant)
            hideViewAnimated(balance_total_staked_container, instant)
            //name_textview.isSingleLine = true
        }
    }

    interface OnItemClickListener {
        fun onRegularBalanceClicked(item: Account)
        fun onShieldedBalanceClicked(item: Account)
    }
}