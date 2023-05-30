package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityDelegationRemoveBinding
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.util.UnitConvertUtil
import java.math.BigInteger

class DelegationRemoveActivity : BaseDelegationBakerActivity() {
    private lateinit var binding: ActivityDelegationRemoveBinding
    private var receiptMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDelegationRemoveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.delegation_remove_delegation_title
        )
        initViews()
    }

    override fun onBackPressed() {
        if (!receiptMode)
            super.onBackPressed()
    }

    override fun initViews() {
        binding.accountToRemoveDelegateFrom.text =
            (viewModel.bakerDelegationData.account?.name ?: "").plus("\n\n")
                .plus(viewModel.bakerDelegationData.account?.address ?: "")
        binding.estimatedTransactionFee.text = ""

        binding.submitDelegationTransaction.setOnClickListener {
            onContinueClicked()
        }

        binding.submitDelegationFinish.setOnClickListener {
            showNotice()
        }

        initializeWaitingLiveData(binding.includeProgress.progressLayout)
        initializeTransactionFeeLiveData(
            binding.includeProgress.progressLayout,
            binding.estimatedTransactionFee
        )
        initializeShowAuthenticationLiveData()

        viewModel.transactionSuccessLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showPageAsReceipt()
            }
        })

        viewModel.loadTransactionFee(true)

        viewModel.loadChainParameters()
    }

    private fun onContinueClicked() {
        validate()
    }

    private fun validate() {
        if (viewModel.atDisposal() < (viewModel.bakerDelegationData.cost ?: BigInteger.ZERO)) {
            showNotEnoughFunds()
        } else {
            if (viewModel.bakerDelegationData.isBakerPool) {
                viewModel.bakerDelegationData.account?.accountDelegation?.delegationTarget?.bakerId?.let {
                    viewModel.setPoolID(it.toString())
                }
            }
            viewModel.bakerDelegationData.amount = BigInteger.ZERO
            viewModel.prepareTransaction()
        }
    }

    override fun errorLiveData(value: Int) {
        Toast.makeText(this, getString(value), Toast.LENGTH_SHORT).show()
    }

    private fun showPageAsReceipt() {
        receiptMode = true
        hideActionBarBack()
        binding.delegationRemoveText.visibility = View.GONE
        binding.submitDelegationTransaction.visibility = View.GONE
        binding.submitDelegationFinish.visibility = View.VISIBLE
        binding.includeTransactionSubmittedHeader.transactionSubmitted.visibility = View.VISIBLE
        viewModel.bakerDelegationData.submissionId?.let {
            binding.includeTransactionSubmittedNo.transactionSubmittedDivider.visibility =
                View.VISIBLE
            binding.includeTransactionSubmittedNo.transactionSubmittedId.visibility = View.VISIBLE
            binding.includeTransactionSubmittedNo.transactionSubmittedId.text = it
        }
    }

    private fun showNotice() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_notice_title)
        val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(
            viewModel.bakerDelegationData.chainParameters?.delegatorCooldown ?: 0
        )
        builder.setMessage(
            resources.getQuantityString(
                R.plurals.delegation_notice_message_remove,
                gracePeriod,
                gracePeriod
            )
        )
        builder.setPositiveButton(getString(R.string.delegation_notice_ok)) { dialog, _ ->
            dialog.dismiss()
            finishUntilClass(AccountDetailsActivity::class.java.canonicalName)
        }
        builder.create().show()
    }

    override fun showWaiting(progressLayout: View, waiting: Boolean) {
        super.showWaiting(progressLayout, waiting)
        binding.submitDelegationTransaction.isEnabled = !waiting
    }
}
