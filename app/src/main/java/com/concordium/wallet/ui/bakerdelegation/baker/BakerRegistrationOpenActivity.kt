package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_baker_registration_open.*

class BakerRegistrationOpenActivity :
    BaseDelegationBakerActivity(R.layout.activity_baker_registration_open, R.string.baker_registration_open_title) {

    override fun initViews() {
        super.initViews()

        viewModel.bakerDelegationData.oldMetadataUrl = viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.metadataUrl

        if (viewModel.bakerDelegationData.type == UPDATE_BAKER_POOL) {
            setActionBarTitle(R.string.baker_update_pool_settings_title)
            open_url_explain.text = getString(R.string.baker_update_pool_settings_open_url_explain)
            viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.metadataUrl?.let {
                current_url.text = getString(R.string.baker_update_pool_settings_current_url, it)
                current_url.visibility = View.VISIBLE
                open_url.setText(it)
            }
        }

        baker_registration_open_continue.setOnClickListener {
            KeyboardUtil.hideKeyboard(this)
            viewModel.bakerDelegationData.metadataUrl = open_url.text?.toString()
            validate()
        }
    }

    private fun validate() {
        var gotoNextPage = false
        if (viewModel.bakerDelegationData.oldMetadataUrl != viewModel.bakerDelegationData.metadataUrl ||
            viewModel.bakerDelegationData.oldOpenStatus != viewModel.bakerDelegationData.bakerPoolInfo?.openStatus)
            gotoNextPage = true

        if (gotoNextPage) gotoNextPage()
        else showNoChange()
    }

    private fun gotoNextPage() {
        val intent = if (viewModel.bakerDelegationData.type == UPDATE_BAKER_POOL) {
            Intent(this, BakerRegistrationConfirmationActivity::class.java)
        } else {
            Intent(this, BakerRegistrationCloseActivity::class.java)
        }
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
