package com.concordium.wallet.ui.bakerdelegation.baker

import android.annotation.SuppressLint
import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import kotlinx.android.synthetic.main.activity_baker_settings.bakerRegistrationContinue
import kotlinx.android.synthetic.main.activity_baker_settings.bakingValue
import kotlinx.android.synthetic.main.activity_baker_settings.transactionFeeValue

class BakerPoolSettingsActivity : BaseDelegationBakerActivity
    (R.layout.activity_baker_settings, R.string.baker_registration_title) {

    override fun initViews() {
        setCommissionRates()
    }

    @SuppressLint("SetTextI18n")
    private fun setCommissionRates() {
        val chainParams = viewModel.bakerDelegationData.chainParameters ?: return
        transactionFeeValue.text = "${
            viewModel.getTransactionRate(
                chainParams.transactionCommissionRange.max,
                chainParams.transactionCommissionRange.min
            ) * 100
        }%"
        bakingValue.text = "${
            viewModel.getTransactionRate(
                chainParams.bakingCommissionRange.max,
                chainParams.bakingCommissionRange.min
            ) * 100
        }%"

        bakerRegistrationContinue.setOnClickListener {
            onContinueClicked()
        }
    }

    private fun onContinueClicked() {
        val intent = Intent(this, BakerRegistrationOpenActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun errorLiveData(value: Int) {
    }
}
