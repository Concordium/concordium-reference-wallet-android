package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.common.GenericFlowActivity
import kotlinx.android.synthetic.main.activity_baker_registration_open.*

class BakerRegistrationOpenActivity :
    BaseDelegationBakerActivity(R.layout.activity_baker_registration_open, R.string.baker_registration_open_title) {

    override fun initViews() {
        super.initViews()

        baker_registration_open_continue.setOnClickListener {
            viewModel.bakerDelegationData.metadataUrl = open_url.text?.toString()
            continueToGenerateKeys()
        }
    }

    private fun continueToGenerateKeys() {
        val intent = Intent(this, BakerRegistrationCloseActivity::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun transactionSuccessLiveData() {
    }

    override fun errorLiveData(value: Int) {
    }

    override fun showDetailedLiveData(value: Boolean) {
    }
}
