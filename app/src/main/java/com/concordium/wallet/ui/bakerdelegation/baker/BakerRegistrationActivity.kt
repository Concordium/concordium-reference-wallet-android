package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.uicore.view.SegmentedControlView
import kotlinx.android.synthetic.main.activity_baker_registration.*

class BakerRegistrationActivity :
    BaseDelegationBakerActivity(R.layout.activity_baker_registration, R.string.baker_registration_title) {

    private lateinit var openForDelegationControl: View
    private lateinit var closeForDelegationControl: View

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
        closeForDelegationControl = baker_options.addControl(
            getString(R.string.baker_registration_close),
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
        val intent = if (viewModel.isClosedBaker())
            Intent(this, BakerRegistrationCloseActivity::class.java)
        else
            Intent(this, BakerRegistrationOpenActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}