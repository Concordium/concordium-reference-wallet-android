package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakerPoolInfo
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_CLOSED_FOR_ALL
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_OPEN_FOR_ALL
import com.concordium.wallet.databinding.ActivityBakerRegistrationBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.uicore.view.SegmentedControlView

class BakerRegistrationActivity : BaseDelegationBakerActivity() {
    private lateinit var binding: ActivityBakerRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.baker_registration_title
        )
        initViews()
    }

    override fun initViews() {
        binding.bakerOptions.clearAll()
        binding.bakerOptions.addControl(
            getString(R.string.baker_registration_open),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_OPEN_FOR_ALL))
                }
            },
            viewModel.bakerDelegationData.bakerPoolStatus?.poolInfo?.openStatus == OPEN_STATUS_OPEN_FOR_ALL || viewModel.bakerDelegationData.bakerPoolStatus?.poolInfo?.openStatus == null
        )
        binding.bakerOptions.addControl(
            getString(R.string.baker_registration_close),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_CLOSED_FOR_ALL))
                }
            },
            viewModel.bakerDelegationData.bakerPoolStatus?.poolInfo?.openStatus == OPEN_STATUS_CLOSED_FOR_ALL
        )

        binding.bakerRegistrationContinue.setOnClickListener {
            onContinueClicked()
        }
    }

    private fun onContinueClicked() {
        val intent =
            if (viewModel.bakerDelegationData.bakerPoolInfo?.openStatus == OPEN_STATUS_CLOSED_FOR_ALL)
                Intent(this, BakerRegistrationCloseActivity::class.java)
            else
                Intent(this, BakerPoolSettingsActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun errorLiveData(value: Int) {
    }
}
