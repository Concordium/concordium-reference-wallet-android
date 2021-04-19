package com.concordium.wallet.ui.account.accountdetails

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.concordium.wallet.R
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.account.accountdetails.identityattributes.AccountDetailsIdentityFragment
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsErrorFragment
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsPendingFragment
import com.concordium.wallet.ui.account.accountdetails.transfers.AccountDetailsTransfersFragment


class AccountDetailsPagerAdapter(
    fragmentManager: FragmentManager, val account: Account,
    val context: Context
) :
    FragmentPagerAdapter(
        fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {


    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> getFirstPositionFragment()
            else -> getSecondPositionFragment()
        }
    }

    override fun getCount(): Int {
        return 2
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
                var fragment =
                    AccountDetailsIdentityFragment()
                var bundle = Bundle()
                bundle.putSerializable(AccountDetailsIdentityFragment.EXTRA_ACCOUNT, account)
                fragment.arguments = bundle
                fragment
            }
        }
    }


    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> context.getString(R.string.account_details_transfers_title)
            else -> context.getString(R.string.account_details_identity_data_title)
        }
    }
}