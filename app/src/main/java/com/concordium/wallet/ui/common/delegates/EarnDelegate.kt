package com.concordium.wallet.ui.common.delegates

import android.app.Activity
import android.content.Intent
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.account.earn.EarnInfoActivity
import com.concordium.wallet.ui.account.earn.EarnInfoActivity.Companion.EXTRA_ACCOUNT_DATA
import com.concordium.wallet.ui.bakerdelegation.baker.BakerStatusActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationStatusActivity

interface EarnDelegate {
    fun gotoEarn(
        activity: Activity,
        account: Account,
        hasPendingDelegationTransactions: Boolean,
        hasPendingBakingTransactions: Boolean
    )
}

class EarnDelegateImpl : EarnDelegate {
    override fun gotoEarn(
        activity: Activity,
        account: Account,
        hasPendingDelegationTransactions: Boolean,
        hasPendingBakingTransactions: Boolean
    ) {
        val intent: Intent
        if (account.accountDelegation != null || hasPendingDelegationTransactions) {
            intent = Intent(activity, DelegationStatusActivity::class.java)
            intent.putExtra(
                EXTRA_DELEGATION_BAKER_DATA,
                BakerDelegationData(
                    account,
                    isTransactionInProgress = hasPendingDelegationTransactions,
                    type = ProxyRepository.UPDATE_DELEGATION
                )
            )

        } else if (account.accountBaker != null || hasPendingBakingTransactions) {
            intent = Intent(activity, BakerStatusActivity::class.java)
            intent.putExtra(
                EXTRA_DELEGATION_BAKER_DATA,
                BakerDelegationData(
                    account,
                    isTransactionInProgress = hasPendingBakingTransactions,
                    type = ProxyRepository.REGISTER_BAKER
                )
            )
        } else {
            intent = Intent(activity, EarnInfoActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT_DATA, account)
        }
        activity.runOnUiThread {
            activity.startActivity(intent)
        }
    }
}
