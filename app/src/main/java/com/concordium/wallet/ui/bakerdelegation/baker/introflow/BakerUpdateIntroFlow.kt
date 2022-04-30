package com.concordium.wallet.ui.bakerdelegation.baker.introflow

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.baker.BakerRegisterAmountActivity
import com.concordium.wallet.ui.bakerdelegation.baker.BakerRegistrationCloseActivity
import com.concordium.wallet.ui.bakerdelegation.baker.BakerStatusActivity.Companion.BAKER_SETTINGS_MENU
import com.concordium.wallet.ui.bakerdelegation.baker.BakerStatusActivity.Companion.BAKER_SETTINGS_MENU_STOP_BAKING
import com.concordium.wallet.ui.bakerdelegation.baker.BakerStatusActivity.Companion.BAKER_SETTINGS_MENU_UPDATE_BAKER_KEYS
import com.concordium.wallet.ui.bakerdelegation.baker.BakerStatusActivity.Companion.BAKER_SETTINGS_MENU_UPDATE_BAKER_STAKE
import com.concordium.wallet.ui.bakerdelegation.baker.BakerStatusActivity.Companion.BAKER_SETTINGS_MENU_UPDATE_POOL_SETTINGS
import com.concordium.wallet.ui.bakerdelegation.baker.BakerUpdatePoolSettingsActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel

class BakerUpdateIntroFlow :
    BaseDelegationBakerFlowActivity(R.string.baker_update_intro_flow_title) {

    override fun getTitles(): IntArray {
        return intArrayOf(
            R.string.baker_update_intro_subtitle1,
            R.string.baker_update_intro_subtitle2,
            R.string.baker_update_intro_subtitle3,
            R.string.baker_update_intro_subtitle4,
            R.string.baker_update_intro_subtitle5)
    }

    override fun gotoContinue() {

        intent.extras?.getInt(BAKER_SETTINGS_MENU)?.let { menuItem ->
            when (menuItem) {
                BAKER_SETTINGS_MENU_UPDATE_BAKER_STAKE -> gotoUpdateBakerStake()
                BAKER_SETTINGS_MENU_UPDATE_POOL_SETTINGS -> gotoUpdatePoolSettings()
                BAKER_SETTINGS_MENU_UPDATE_BAKER_KEYS -> gotoUpdateBakerKeys()
                BAKER_SETTINGS_MENU_STOP_BAKING -> gotoStopBaking()
            }
        }
    }

    override fun getLink(position: Int): String {
        return "file:///android_asset/baker_update_intro_flow_en_"+(position+1)+".html"
    }

    private fun gotoUpdateBakerStake() {
        val intent = Intent(this, BakerRegisterAmountActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun gotoUpdatePoolSettings() {
        val intent = Intent(this, BakerUpdatePoolSettingsActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun gotoUpdateBakerKeys() {
        val intent = Intent(this, BakerRegistrationCloseActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun gotoStopBaking() {
        val intent = Intent(this, BakerRemoveIntroFlow::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
