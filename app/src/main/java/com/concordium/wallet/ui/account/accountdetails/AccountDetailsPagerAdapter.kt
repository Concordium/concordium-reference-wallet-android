package com.concordium.wallet.ui.account.accountdetails

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.account.accountdetails.identityattributes.AccountDetailsIdentityFragment
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsErrorFragment
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsPendingFragment
import com.concordium.wallet.ui.account.accountdetails.transfers.AccountDetailsTransfersFragment

class AccountDetailsPagerAdapter(fragmentManager: FragmentActivity, val account: Account, val context: Context) :
    FragmentStateAdapter(fragmentManager) {

    override fun getItemCount(): Int {
        return 1
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> getFirstPositionFragment()
            else -> getSecondPositionFragment()
        }
    }

    private fun getFirstPositionFragment(): Fragment {
        return when (account.transactionStatus) {
            TransactionStatus.ABSENT -> AccountDetailsErrorFragment()
            TransactionStatus.COMMITTED -> AccountDetailsPendingFragment()
            TransactionStatus.RECEIVED -> AccountDetailsPendingFragment()
            else -> AccountDetailsTransfersFragment()
        }
    }

    private fun getSecondPositionFragment(): Fragment {
        return when (account.transactionStatus) {
            TransactionStatus.ABSENT -> AccountDetailsErrorFragment()
            TransactionStatus.COMMITTED -> AccountDetailsPendingFragment()
            TransactionStatus.RECEIVED -> AccountDetailsPendingFragment()
            else -> {
                val fragment = AccountDetailsIdentityFragment()
                val bundle = Bundle()
                bundle.putSerializable(AccountDetailsIdentityFragment.EXTRA_ACCOUNT, account)
                fragment.arguments = bundle
                fragment
            }
        }
    }
}
