package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import com.concordium.wallet.R
import com.concordium.wallet.data.model.DelegationData
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.AMOUNT_TOO_LARGE_FOR_POOL
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.uicore.view.SegmentedControlView
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_delegation_registration_pool.*

class DelegationRegisterPoolActivity :
    BaseDelegationBakerActivity(R.layout.activity_delegation_registration_pool, R.string.delegation_register_delegation_title) {

    private lateinit var lPoolControl: View
    private lateinit var bakerPoolControl: View

    fun showError() {
        pool_id.setTextColor(getColor(R.color.text_pink))
        pool_id_error.visibility = View.VISIBLE
    }

    private fun hideError() {
        pool_id.setTextColor(getColor(R.color.theme_blue))
        pool_id_error.visibility = View.INVISIBLE
    }

    private fun showDetailedPage() {
        val intent = Intent(this, DelegationRegisterAmountActivity::class.java)
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun initViews() {
        pool_options.clearAll()
        bakerPoolControl = pool_options.addControl(
            getString(R.string.delegation_register_delegation_pool_baker),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    pool_id.setText("")
                    viewModel.selectBakerPool()
                    updateVisibilities()
                }
            },
            viewModel.isBakerPool() || (!viewModel.isBakerPool() && !viewModel.isLPool()))
            lPoolControl = pool_options.addControl(getString(R.string.delegation_register_delegation_passive), object: SegmentedControlView.OnItemClickListener {
                override fun onItemClicked(){
                    viewModel.selectLPool()
                    updateVisibilities()
                }
            }, viewModel.isLPool())

        pool_id.setText(viewModel.getPoolId())
        pool_id.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    KeyboardUtil.hideKeyboard(this)
                    onContinueClicked()
                    true
                }
                false
            }

        pool_registration_continue.setOnClickListener {
            onContinueClicked()
        }

        pool_id.doOnTextChanged { text, _, _, _ ->
            if (text != null && text.isNotEmpty())
                viewModel.setPoolID(text.toString())
            updateVisibilities()
        }

        updateContent()
        updateVisibilities()

        initializeWaitingLiveData()
        initializeShowDetailedLiveData()
    }

    override fun transactionSuccessLiveData() {
    }

    override fun errorLiveData(value: Int) {
        if (value == AMOUNT_TOO_LARGE_FOR_POOL) {
            showDelegationAmountTooLargeNotice()
        } else {
            pool_id_error.text = getString(value)
            showError()
        }
    }

    override fun showDetailedLiveData(value: Boolean) {
        if (value) {
            showDetailedPage()
        }
    }

    private fun updateContent() {
        if (viewModel.bakerDelegationData.type == DelegationData.TYPE_UPDATE_DELEGATION) {
            setActionBarTitle(R.string.delegation_update_delegation_title)
            viewModel.bakerDelegationData.oldRestake = viewModel.bakerDelegationData.restake
            viewModel.bakerDelegationData.oldDelegationIsBaker = viewModel.isBakerPool()
            viewModel.bakerDelegationData.oldDelegationTargetPoolId = viewModel.bakerDelegationData.account?.accountDelegation?.delegationTarget?.bakerId
            if (viewModel.isBakerPool()) {
                viewModel.selectBakerPool()
                existing_pool_id.text = getString(R.string.delegation_update_delegation_pool_id_baker, getExistingPoolIdText())
            } else {
                viewModel.selectLPool()
                existing_pool_id.text = getString(R.string.delegation_update_delegation_pool_id__passive)
            }
        }
    }

    private fun updateVisibilities() {
        pool_id.hint = if (viewModel.bakerDelegationData.oldDelegationTargetPoolId == null) getString(R.string.delegation_register_delegation_pool_id_hint) else getString(R.string.delegation_register_delegation_pool_id_hint_update)
        pool_id.visibility = if (viewModel.bakerDelegationData.isLPool) View.GONE else View.VISIBLE
        pool_desc.text = if (viewModel.bakerDelegationData.isLPool) getString(R.string.delegation_register_delegation_desc_passive) else getString(R.string.delegation_register_delegation_desc)
        pool_registration_continue.isEnabled = getExistingPoolIdText().isNotEmpty() || viewModel.bakerDelegationData.isLPool || pool_id.text.isNotEmpty()
        hideError()
    }

    private fun onContinueClicked() {
        if (viewModel.isBakerPool() && viewModel.getPoolId().isEmpty() && getExistingPoolIdText().isNotEmpty())
            viewModel.setPoolID(getExistingPoolIdText())
        viewModel.validatePoolId()
    }

    private fun getExistingPoolIdText(): String {
        viewModel.bakerDelegationData.account?.accountDelegation?.delegationTarget?.bakerId?.let {
            return it.toString()
        }
        return ""
    }

    private fun showDelegationAmountTooLargeNotice() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delegation_amount_too_large_notice_title)
        builder.setMessage(getString(R.string.delegation_amount_too_large_notice_message))
        builder.setPositiveButton(getString(R.string.delegation_amount_too_large_notice_lower)) { _, _ -> showDetailedPage() }
        builder.setNegativeButton(getString(R.string.delegation_amount_too_large_notice_stop)) { _, _ -> gotoStopDelegation() }
        builder.setNeutralButton(getString(R.string.delegation_amount_too_large_notice_cancel)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun gotoStopDelegation() {
        val intent = Intent(this, DelegationRemoveActivity::class.java)
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
