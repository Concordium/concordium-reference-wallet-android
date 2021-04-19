package com.concordium.wallet.ui.account.accountdetails

import android.os.Bundle
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_account_transaction_filters.*


class AccountTransactionsFiltersActivity :
    BaseActivity(R.layout.activity_account_transaction_filters, R.string.account_transaction_filters) {

    private val session: Session = App.appCore.session

    private lateinit var mAccount: Account

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAccount = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        initializeViewModel()
        initViews()
    }

    override fun onResume() {
        super.onResume()

        filter_show_rewards.isChecked = session.getHasShowRewards(mAccount.id)
        filter_show_finalizations_rewards.isChecked = session.getHasShowFinalizationRewards(mAccount.id)
        filter_show_finalizations_rewards.isEnabled = filter_show_rewards.isChecked

        filter_show_rewards.setOnCheckedChangeListener { _, isChecked ->
            session.setHasShowRewards(mAccount.id, isChecked)
            if(!isChecked){
                filter_show_finalizations_rewards.isEnabled = false
                filter_show_finalizations_rewards.isChecked = false
                session.setHasShowFinalizationRewards(mAccount.id, false)
            }
            else{
                filter_show_finalizations_rewards.isEnabled = true
            }
        }
        filter_show_finalizations_rewards.setOnCheckedChangeListener { _, isChecked ->
            session.setHasShowFinalizationRewards(mAccount.id, isChecked)
        }

    }

    // endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {


    }

    private fun initViews() {
    }

    //endregion

    //region Control/UI
    //************************************************************


    //endregion
}

