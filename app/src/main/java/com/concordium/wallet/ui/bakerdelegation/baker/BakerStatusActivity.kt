package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerRemoveIntroFlow
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerUpdateIntroFlow
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.common.StatusActivity
import com.concordium.wallet.ui.common.GenericFlowActivity
import kotlinx.android.synthetic.main.delegationbaker_status.*

class BakerStatusActivity :
    StatusActivity(R.string.baker_status_title) {

    private var menuDialog: AlertDialog? = null

    companion object {
        const val BAKER_SETTINGS_MENU = "BAKER_SETTINGS_MENU"
        const val BAKER_SETTINGS_MENU_UPDATE_BAKER_STAKE = 1
        const val BAKER_SETTINGS_MENU_UPDATE_POOL_SETTINGS = 2
        const val BAKER_SETTINGS_MENU_UPDATE_BAKER_KEYS = 3
        const val BAKER_SETTINGS_MENU_STOP_BAKING = 4
    }

    override fun initView() {
        status_button_bottom.visibility = View.VISIBLE

        if (viewModel.bakerDelegationData.account?.isBaking() == true) {
            status_button_bottom.text = getString(R.string.baker_status_update_baker_settings)
            status_button_bottom.setOnClickListener {
                openUpdateBakerSettingsMenu()
            }
        } else {
            setEmptyState(getString(R.string.baker_status_no_baker))
            status_button_bottom.text = getString(R.string.baker_status_register_baker)
            status_button_bottom.setOnClickListener {
                continueToBakerAmount()
            }
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
        builder.setOnDismissListener {

        }

        val menuView = View.inflate(this, R.layout.menu_update_baker_settings_content, null)

        val menuItemUpdateBakerStake = menuView.findViewById(R.id.menu_item_update_baker_stake) as TextView
        menuItemUpdateBakerStake.setOnClickListener {
            gotoBakerUpdateIntroFlow(BAKER_SETTINGS_MENU_UPDATE_BAKER_STAKE)
        }

        val menuItemUpdatePolSettings = menuView.findViewById(R.id.menu_item_update_pool_settings) as TextView
        menuItemUpdatePolSettings.setOnClickListener {
            gotoBakerUpdateIntroFlow(BAKER_SETTINGS_MENU_UPDATE_POOL_SETTINGS)
        }

        val menuItemUpdateBakerKeys = menuView.findViewById(R.id.menu_item_update_baker_keys) as TextView
        menuItemUpdateBakerKeys.setOnClickListener {
            gotoBakerUpdateIntroFlow(BAKER_SETTINGS_MENU_UPDATE_BAKER_KEYS)
        }

        val menuItemStopBaking = menuView.findViewById(R.id.menu_item_stop_baking) as TextView
        menuItemStopBaking.setOnClickListener {
            gotoBakerRemoveIntroFlow()
        }

        builder.setCustomTitle(menuView)
        menuDialog = builder.show()

        menuDialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun gotoBakerUpdateIntroFlow(bakerSettingsMenuItem: Int) {
        menuDialog?.dismiss()
        val intent = Intent(this, BakerUpdateIntroFlow::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        intent.putExtra(BAKER_SETTINGS_MENU, bakerSettingsMenuItem)
        startActivity(intent)
    }

    private fun gotoBakerRemoveIntroFlow() {
        menuDialog?.dismiss()
        val intent = Intent(this, BakerRemoveIntroFlow::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivity(intent)
    }
}
