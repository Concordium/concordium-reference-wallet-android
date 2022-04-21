package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.delegation.BaseDelegationActivity
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationRegisterAmountActivity
import com.concordium.wallet.uicore.view.SegmentedControlView
import kotlinx.android.synthetic.main.activity_baker_registration.*

class BakerRegistrationActivity :
    BaseBakerActivity(R.layout.activity_baker_registration, R.string.baker_registration_title) {

    private lateinit var openForDelegationControl: View
    private lateinit var closedForDelegationControl: View

    override fun initViews() {
        baker_options.clearAll()
        openForDelegationControl = baker_options.addControl(
            getString(R.string.baker_registration_open),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenBaker()

                }
            },
            viewModel.isOpenBaker()
        )
        closedForDelegationControl = baker_options.addControl(
            getString(R.string.baker_registration_closed),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectClosedBaker()

                }
            },
            viewModel.isClosedBaker()
        )

        baker_registration_continue.setOnClickListener {
            onContinueClicked()
        }
    }

    private fun onContinueClicked() {
        val intent = Intent(this, BakerGenerateKeysAndExportActivity::class.java)
        intent.putExtra(EXTRA_BAKER_DATA, viewModel.bakerData)
        startActivityForResultAndHistoryCheck(intent)
    }
}