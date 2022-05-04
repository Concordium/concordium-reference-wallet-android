package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REMOVE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_KEYS
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_STAKE
import com.concordium.wallet.data.model.BakerPoolInfo
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerRemoveIntroFlow
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerUpdateIntroFlow
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.common.StatusActivity
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.util.DateTimeUtil.formatTo
import com.concordium.wallet.util.DateTimeUtil.toDate
import kotlinx.android.synthetic.main.delegationbaker_status.*

class BakerStatusActivity :
    StatusActivity(R.string.baker_status_title) {

    private var menuDialog: AlertDialog? = null

    override fun initView() {

        clearState()

        val account = viewModel.bakerDelegationData.account
        val accountBaker = account?.accountBaker

        if (account == null || accountBaker == null) {
            findViewById<ImageView>(R.id.status_icon).setImageResource(R.drawable.ic_logo_icon_pending)
            setContentTitle(R.string.baker_status_no_baker_title)
            setEmptyState(getString(R.string.baker_status_no_baker))
            status_button_bottom.text = getString(R.string.baker_status_register_baker)
            status_button_bottom.setOnClickListener {
                continueToBakerAmount()
            }
            return
        }

        status_button_bottom.text = getString(R.string.baker_status_update_baker_settings)

        if (viewModel.bakerDelegationData.isTransactionInProgress) {
            findViewById<ImageView>(R.id.status_icon).setImageResource(R.drawable.ic_logo_icon_pending)
            setContentTitle(R.string.baker_status_baker_waiting_title)
            setEmptyState(getString(R.string.baker_status_baker_waiting))
            status_button_bottom.isEnabled = false
            return
        }

        findViewById<ImageView>(R.id.status_icon).setImageResource(R.drawable.ic_big_logo_ok)
        setContentTitle(R.string.baker_status_baker_registered_title)

        addContent(R.string.baker_status_baker_account, account.name + "\n\n" + account.address)
        addContent(R.string.baker_status_baker_stake, CurrencyUtil.formatGTU(accountBaker.stakedAmount, true))
        addContent(R.string.baker_status_baker_id, accountBaker.bakerId.toString())

        if (accountBaker.restakeEarnings) addContent(R.string.baker_status_baker_rewards_will_be, getString(R.string.baker_status_baker_added_to_stake))
        else addContent(R.string.baker_status_baker_rewards_will_be, getString(R.string.baker_status_baker_at_disposal))

        when (accountBaker.bakerPoolInfo.openStatus) {
            BakerPoolInfo.OPEN_STATUS_OPEN_FOR_ALL -> addContent(R.string.baker_status_baker_delegation_pool_status, getString(R.string.baker_status_baker_delegation_pool_status_open))
            BakerPoolInfo.OPEN_STATUS_CLOSED_FOR_NEW -> addContent(R.string.baker_status_baker_delegation_pool_status, getString(R.string.baker_status_baker_delegation_pool_status_closed_for_new))
            else -> addContent(R.string.baker_status_baker_delegation_pool_status, getString(R.string.baker_status_baker_delegation_pool_status_closed))
        }

        if (!accountBaker.bakerPoolInfo.metadataUrl.isNullOrBlank()) {
            addContent(R.string.baker_status_baker_metadata_url, accountBaker.bakerPoolInfo.metadataUrl)
        }

        accountBaker.pendingChange?.let { pendingChange ->
            val prefix = pendingChange.effectiveTime.toDate()?.formatTo("yyyy-MM-dd")
            val postfix = pendingChange.effectiveTime.toDate()?.formatTo("HH:mm")
            val dateStr = getString(R.string.baker_status_baker_effective_time, prefix, postfix)
            addContent(getString(R.string.baker_status_baker_take_effect_on) + "\n" + dateStr, "")
            if (pendingChange.change == "RemoveStake") {
                addContent(getString(R.string.baker_status_baker_effective_remove), "")
            } else if (pendingChange.change == "ReduceStake") {
                pendingChange.newStake?.let { newStake ->
                    addContent(getString(R.string.delegation_status_new_amount), CurrencyUtil.formatGTU(newStake, true))
                }
            }
        }

        status_button_bottom.setOnClickListener {
            openUpdateBakerSettingsMenu()
        }
    }

    private fun continueToBakerAmount() {
        val intent = Intent(this, BakerRegisterAmountActivity::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun openUpdateBakerSettingsMenu() {

        val builder = AlertDialog.Builder(this)
        val menuView = View.inflate(this, R.layout.menu_update_baker_settings_content, null)

        val menuItemUpdateBakerStake = menuView.findViewById(R.id.menu_item_update_baker_stake) as TextView
        menuItemUpdateBakerStake.setOnClickListener {
            gotoBakerUpdateIntroFlow(UPDATE_BAKER_STAKE)
        }

        val menuItemUpdatePolSettings = menuView.findViewById(R.id.menu_item_update_pool_settings) as TextView
        menuItemUpdatePolSettings.setOnClickListener {
            gotoBakerUpdateIntroFlow(UPDATE_BAKER_POOL)
        }

        val menuItemUpdateBakerKeys = menuView.findViewById(R.id.menu_item_update_baker_keys) as TextView
        menuItemUpdateBakerKeys.setOnClickListener {
            gotoBakerUpdateIntroFlow(UPDATE_BAKER_KEYS)
        }

        val menuItemStopBaking = menuView.findViewById(R.id.menu_item_stop_baking) as TextView
        menuItemStopBaking.setOnClickListener {
            gotoBakerRemoveIntroFlow()
        }

        builder.setCustomTitle(menuView)
        menuDialog = builder.show()

        menuDialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun gotoBakerUpdateIntroFlow(bakerSettingsMenuItem: String) {
        menuDialog?.dismiss()
        val intent = Intent(this, BakerUpdateIntroFlow::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        viewModel.bakerDelegationData.type = bakerSettingsMenuItem
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun gotoBakerRemoveIntroFlow() {
        menuDialog?.dismiss()
        val intent = Intent(this, BakerRemoveIntroFlow::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        viewModel.bakerDelegationData.type = REMOVE_BAKER
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
