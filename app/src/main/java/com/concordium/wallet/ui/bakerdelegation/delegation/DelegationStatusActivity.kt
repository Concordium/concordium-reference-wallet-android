package com.concordium.wallet.ui.bakerdelegation.delegation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.DelegationData
import com.concordium.wallet.data.model.DelegationTarget
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.bakerdelegation.common.StatusActivity
import com.concordium.wallet.ui.bakerdelegation.delegation.introflow.DelegationRemoveIntroFlowActivity
import com.concordium.wallet.ui.bakerdelegation.delegation.introflow.DelegationUpdateIntroFlowActivity
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.util.DateTimeUtil.formatTo
import com.concordium.wallet.util.DateTimeUtil.toDate
import kotlinx.android.synthetic.main.delegationbaker_status.*

class DelegationStatusActivity :
    StatusActivity(R.string.delegation_status_title) {

    private lateinit var viewModel: DelegationViewModel

    companion object {
        const val EXTRA_DELEGATION_DATA = "EXTRA_DELEGATION_DATA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize(intent.extras?.getSerializable(EXTRA_DELEGATION_DATA) as DelegationData)
        viewModel.loadChainParameters()
        initView()
    }

    fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(DelegationViewModel::class.java)
    }

    override fun initView() {

        val account = viewModel.delegationData.account
        val accountDelegation = account?.accountDelegation

        //TODO update with proper status info
        // LOAD STATUS FROM WALLET-PROXY ?
        // val w = getString(R.string.delegation_status_waiting_to_finalize)

        if (account == null || accountDelegation == null) {
            findViewById<ImageView>(R.id.status_icon).setImageResource(R.drawable.ic_logo_icon_pending)
            setContentTitle(R.string.delegation_status_content_empty_title)
            setEmptyState(getString(R.string.delegation_status_content_empty_desc))

            status_button_bottom.visibility = View.VISIBLE
            status_button_bottom.text = getString(R.string.delegation_status_continue)
            status_button_bottom.setOnClickListener {
                continueToCreate()
            }
        }
        else {
            findViewById<ImageView>(R.id.status_icon).setImageResource(R.drawable.ic_big_logo_ok)
            setContentTitle(R.string.delegation_status_content_registered_title)
            addContent(R.string.delegation_status_content_delegating_account, account.name + "\n\n" + account.address)
            addContent(R.string.delegation_status_content_delegation_amount, CurrencyUtil.formatGTU(accountDelegation.stakedAmount, true))
            if (accountDelegation.delegationTarget.delegateType == DelegationTarget.TYPE_DELEGATE_TO_BAKER) {
                addContent(R.string.delegation_status_content_target_pool, accountDelegation.delegationTarget.bakerId.toString())
            }
            else {
                addContent(R.string.delegation_status_content_target_pool, DelegationTarget.TYPE_DELEGATE_TO_L_POOL)
            }

            if (accountDelegation.restakeEarnings) addContent(R.string.delegation_status_content_rewards_will_be, getString(R.string.delegation_status_added_to_delegation_amount))
            else addContent(R.string.delegation_status_content_rewards_will_be, getString(R.string.delegation_status_at_disposal))

            viewModel.delegationData.account?.accountDelegation?.pendingChange?.let { pendingChange ->
                val prefix = pendingChange.effectiveTime.toDate()?.formatTo("yyyy-MM-dd")
                val postfix = pendingChange.effectiveTime.toDate()?.formatTo("HH:mm")
                val dateStr = getString(R.string.delegation_status_effective_time, prefix, postfix)
                addContent(getString(R.string.delegation_status_content_take_effect_on) + "\n" + dateStr, "")
                if (pendingChange.change == "RemoveStake") {
                    status_button_top.isEnabled = false
                    addContent(getString(R.string.delegation_status_content_delegation_will_be_stopped), "")
                } else if (pendingChange.change == "ReduceStake") {
                    pendingChange.newStake?.let { newStake ->
                        addContent(getString(R.string.delegation_status_new_amount), CurrencyUtil.formatGTU(newStake, true))
                    }
                }
            }

            status_button_top.visibility = View.VISIBLE
            status_button_top.text = getString(R.string.delegation_status_stop)
            status_button_top.setOnClickListener {
                continueToDelete()
            }

            status_button_bottom.visibility = View.VISIBLE
            status_button_bottom.text = getString(R.string.delegation_status_update)
            status_button_bottom.setOnClickListener {
                continueToUpdate()
            }
        }
    }

    private fun continueToDelete(){
        val intent = Intent(this, DelegationRemoveIntroFlowActivity::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        viewModel.delegationData.type = DelegationData.TYPE_REMOVE_DELEGATION
        intent.putExtra(BaseDelegationBakerFlowActivity.EXTRA_DELEGATION_DATA, viewModel.delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun continueToCreate() {
        val intent = Intent(this, DelegationRegisterPoolActivity::class.java)
        viewModel.delegationData.type = DelegationData.TYPE_REGISTER_DELEGATION
        intent.putExtra(EXTRA_DELEGATION_DATA, viewModel.delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun continueToUpdate() {
        val intent = Intent(this, DelegationUpdateIntroFlowActivity::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        viewModel.delegationData.type = DelegationData.TYPE_UPDATE_DELEGATION
        intent.putExtra(BaseDelegationBakerFlowActivity.EXTRA_DELEGATION_DATA, viewModel.delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
