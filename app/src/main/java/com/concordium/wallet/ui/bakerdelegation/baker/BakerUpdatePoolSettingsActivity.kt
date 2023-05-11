package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakerPoolInfo
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_CLOSED_FOR_ALL
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_CLOSED_FOR_NEW
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_OPEN_FOR_ALL
import com.concordium.wallet.databinding.ActivityBakerUpdatePoolSettingsBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.uicore.view.SegmentedControlView

class BakerUpdatePoolSettingsActivity : BaseDelegationBakerActivity() {
    private lateinit var binding: ActivityBakerUpdatePoolSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerUpdatePoolSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.baker_update_pool_settings_title
        )
        initViews()
    }

    override fun initViews() {
        super.initViews()

        viewModel.bakerDelegationData.oldOpenStatus =
            viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.openStatus

        viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.let {
            viewModel.selectOpenStatus(it)
        }

        binding.poolOptions.clearAll()

        binding.poolOptions.addControl(
            getString(R.string.baker_update_pool_settings_option_open),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_OPEN_FOR_ALL))
                }
            },
            viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.openStatus == OPEN_STATUS_OPEN_FOR_ALL
        )
        if (viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.openStatus != OPEN_STATUS_CLOSED_FOR_ALL) {
            binding.poolOptions.addControl(
                getString(R.string.baker_update_pool_settings_option_close_for_new),
                object : SegmentedControlView.OnItemClickListener {
                    override fun onItemClicked() {
                        viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_CLOSED_FOR_NEW))
                    }
                },
                viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.openStatus == OPEN_STATUS_CLOSED_FOR_NEW
            )
        }
        binding.poolOptions.addControl(
            getString(R.string.baker_update_pool_settings_option_close),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_CLOSED_FOR_ALL))
                }
            },
            viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.openStatus == OPEN_STATUS_CLOSED_FOR_ALL
        )

        when (viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.openStatus) {
            OPEN_STATUS_OPEN_FOR_ALL -> binding.poolSettingsCurrentStatus.text =
                getString(R.string.baker_update_pool_settings_current_status_open)

            OPEN_STATUS_CLOSED_FOR_NEW -> binding.poolSettingsCurrentStatus.text =
                getString(R.string.baker_update_pool_settings_current_status_closed_for_new)

            else -> binding.poolSettingsCurrentStatus.text =
                getString(R.string.baker_update_pool_settings_current_status_closed)
        }

        binding.updatePoolSettingsContinue.setOnClickListener {
            gotoNextPage()
        }
    }

    private fun gotoNextPage() {
        val intent = Intent(this, BakerRegistrationOpenActivity::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun errorLiveData(value: Int) {
    }
}
