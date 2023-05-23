package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.databinding.ActivityBakerRegistrationOpenBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.uicore.handleUrlClicks
import com.concordium.wallet.util.KeyboardUtil

class BakerRegistrationOpenActivity : BaseDelegationBakerActivity() {
    private lateinit var binding: ActivityBakerRegistrationOpenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerRegistrationOpenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.baker_registration_open_title
        )
        initViews()
    }

    override fun initViews() {
        super.initViews()

        viewModel.bakerDelegationData.oldMetadataUrl =
            viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.metadataUrl

        if (viewModel.bakerDelegationData.type == UPDATE_BAKER_POOL) {
            setActionBarTitle(R.string.baker_update_pool_settings_title)
            binding.openUrlExplain.setText(R.string.baker_update_pool_settings_open_url_explain)
            viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.metadataUrl?.let {
                binding.currentUrl.text =
                    getString(R.string.baker_update_pool_settings_current_url, it)
                binding.currentUrl.visibility = View.VISIBLE
                binding.openUrl.setText(it)
            }
        }

        binding.openUrlExplain.handleUrlClicks { url ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            ContextCompat.startActivity(this, browserIntent, null)
        }

        binding.bakerRegistrationOpenContinue.setOnClickListener {
            KeyboardUtil.hideKeyboard(this)
            viewModel.bakerDelegationData.metadataUrl = binding.openUrl.text?.toString()
            validate()
        }
    }

    private fun validate() {
        var gotoNextPage = false
        if (viewModel.bakerDelegationData.oldMetadataUrl != viewModel.bakerDelegationData.metadataUrl ||
            viewModel.bakerDelegationData.oldOpenStatus != viewModel.bakerDelegationData.bakerPoolInfo?.openStatus
        )
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
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun errorLiveData(value: Int) {
    }
}
