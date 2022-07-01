package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REMOVE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_KEYS
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_STAKE
import com.concordium.wallet.data.model.BakerPoolInfo
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.MenuUpdateBakerSettingsContentBinding
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerRemoveIntroFlow
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerUpdateIntroFlow
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.common.StatusActivity
import com.concordium.wallet.ui.common.GenericFlowActivity

class BakerStatusActivity : StatusActivity(R.string.baker_status_title) {
    private var menuDialog: AlertDialog? = null

    override fun initView() {
        clearState()

        val account = viewModel.bakerDelegationData.account
        val accountBaker = account?.accountBaker

        binding.statusButtonBottom.text = getString(R.string.baker_status_update_baker_settings)

        if (viewModel.bakerDelegationData.isTransactionInProgress) {
            addWaitingForTransaction(R.string.baker_status_baker_waiting_title, R.string.baker_status_baker_waiting)
            return
        }

        if (account == null || accountBaker == null) {
            binding.statusIcon.setImageResource(R.drawable.ic_logo_icon_pending)
            setContentTitle(R.string.baker_status_no_baker_title)
            setEmptyState(getString(R.string.baker_status_no_baker))
            binding.statusButtonBottom.text = getString(R.string.baker_status_register_baker)
            binding.statusButtonBottom.setOnClickListener {
                continueToBakerAmount()
            }
            return
        }

        binding.statusIcon.setImageResource(R.drawable.ic_big_logo_ok)
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
            addPendingChange(pendingChange, R.string.baker_status_baker_effective_time, R.string.baker_status_baker_take_effect_on, R.string.baker_status_baker_effective_remove, R.string.baker_status_baker_stake_lowered_to)
        }

        binding.statusButtonBottom.setOnClickListener {
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
        val menuView = MenuUpdateBakerSettingsContentBinding.inflate(layoutInflater)

        menuView.menuItemUpdateBakerStake.setOnClickListener {
            gotoBakerUpdateIntroFlow(UPDATE_BAKER_STAKE)
        }

        menuView.menuItemUpdatePoolSettings.setOnClickListener {
            gotoBakerUpdateIntroFlow(UPDATE_BAKER_POOL)
        }

        menuView.menuItemUpdateBakerKeys.setOnClickListener {
            gotoBakerUpdateIntroFlow(UPDATE_BAKER_KEYS)
        }

        if (viewModel.isInCoolDown()) {
            menuView.menuItemStopBaking.isEnabled = false
            menuView.menuItemStopBaking.setTextColor(getColor(R.color.text_grey))
        } else {
            menuView.menuItemStopBaking.setOnClickListener {
                gotoBakerRemoveIntroFlow()
            }
        }

        builder.setCustomTitle(menuView.root)
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
