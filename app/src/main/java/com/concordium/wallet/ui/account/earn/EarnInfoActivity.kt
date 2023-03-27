package com.concordium.wallet.ui.account.earn

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtilImpl
import com.concordium.wallet.databinding.ActivityEarnInfoBinding
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerRegistrationIntroFlow
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.delegation.introflow.DelegationCreateIntroFlowActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.GenericFlowActivity

class EarnInfoActivity : BaseActivity() {
    private lateinit var binding: ActivityEarnInfoBinding
    private lateinit var viewModel: EarnViewModel
    private lateinit var account: Account

    companion object {
        const val EXTRA_ACCOUNT_DATA = "EXTRA_ACCOUNT_DATA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEarnInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.earn_title)
        account = intent.extras?.getSerializable(EXTRA_ACCOUNT_DATA) as Account
        initViews()
        initializeViewModel()
        initObservers()
        showWaiting(true)
        viewModel.loadChainParameters()
    }

    private fun initViews() {
        binding.btnBaker.setOnClickListener {
            val intent = Intent(this, BakerRegistrationIntroFlow::class.java)
            intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
            intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, BakerDelegationData(account, type = ProxyRepository.REGISTER_BAKER))
            startActivityForResultAndHistoryCheck(intent)
        }
        binding.btnDelegation.setOnClickListener {
            val intent = Intent(this, DelegationCreateIntroFlowActivity::class.java)
            intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
            intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, BakerDelegationData(account, type = ProxyRepository.REGISTER_DELEGATION))
            startActivityForResultAndHistoryCheck(intent)
        }
    }

    private fun initObservers() {
        viewModel.chainParameters.observe(this) { chainParameters ->
            chainParameters?.let {
                val minimum = CurrencyUtilImpl.formatGTU(chainParameters.minimumEquityCapital, true)
                binding.tvBakerDescription.text = getString(R.string.earn_baker_description, minimum)
                showWaiting(false)
                binding.scrollViewInfo.visibility = View.VISIBLE
            }
        }
        viewModel.error.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showWaiting(false)
                showError(value)
            }
        })
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(binding.root, stringRes)
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[EarnViewModel::class.java]
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressLayout.visibility = if (waiting) View.VISIBLE else View.GONE
    }
}
