package com.concordium.wallet.ui.bakerdelegation.delegation

import android.content.Intent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import com.concordium.wallet.R
import com.concordium.wallet.data.model.DelegationData
import com.concordium.wallet.data.model.DelegationTarget
import com.concordium.wallet.uicore.view.SegmentedControlView
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_delegation_registration_pool.*

class DelegationRegisterPoolActivity :
    BaseDelegationActivity(R.layout.activity_delegation_registration_pool, R.string.delegation_register_delegation_title) {

    private lateinit var lPoolControl: View
    private lateinit var bakerPoolControl: View

    override fun initializeViewModel() {
        super.initializeViewModel()
        initializeWaitingLiveData()
        initializeShowDetailedLiveData()
    }

    fun showError() {
        pool_id.setTextColor(getColor(R.color.text_pink))
        pool_id_error.visibility = View.VISIBLE
    }

    private fun hideError() {
        pool_id.setTextColor(getColor(R.color.theme_blue))
        pool_id_error.visibility = View.GONE
    }

    private fun showDetailedPage() {
        val intent = Intent(this, DelegationRegisterAmountActivity::class.java)
        intent.putExtra(EXTRA_DELEGATION_DATA, viewModel.delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun initViews() {
        pool_options.clearAll()
        bakerPoolControl = pool_options.addControl(
            getString(R.string.delegation_register_delegation_pool_baker),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectBakerPool()
                    updateVisibilities()
                }
            },
            viewModel.isBakerPool()
        )
        lPoolControl = pool_options.addControl(getString(R.string.delegation_register_delegation_pool_l), object: SegmentedControlView.OnItemClickListener {
            override fun onItemClicked(){
                viewModel.selectLPool()
                updateVisibilities()
            }
        }, viewModel.isLPool())

        pool_id.setText(viewModel.getPoolId())
        pool_id.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onContinueClicked()
                    true
                }
                false
            }

        pool_registration_continue.setOnClickListener {
            onContinueClicked()
        }

        pool_id.doOnTextChanged { text, start, count, after ->
            updateVisibilities()
        }

        updateVisibilities()
        updateContent()
    }

    override fun transactionSuccessLiveData() {
    }

    override fun errorLiveData(value: Int) {
        showError()
    }

    override fun showDetailedLiveData(value: Boolean) {
        if (value) {
            showDetailedPage()
        }
    }

    private fun updateContent() {
        if(viewModel.delegationData.type == DelegationData.TYPE_UPDATE_DELEGATION){
            existing_pool_id.text = getString(R.string.delegation_update_delegation_pool_id, getExistingPoolIdText())
            existing_pool_id.visibility = View.VISIBLE
            pool_id.hint = getString(R.string.delegation_register_delegation_pool_id_hint_update)
            if(viewModel.delegationData.account?.accountDelegation?.delegationTarget?.delegateType == DelegationTarget.TYPE_DELEGATE_TO_L_POOL){
                viewModel.selectLPool()
                lPoolControl.isSelected
            }
            else{
                viewModel.selectBakerPool()
                bakerPoolControl.isSelected
            }
            setActionBarTitle(R.string.delegation_update_delegation_title)
            viewModel.setOldPoolID(getExistingPoolIdText())
        }
        else{
            existing_pool_id.visibility = View.GONE
        }
    }

    private fun updateVisibilities() {
        pool_id.visibility = if(viewModel.isLPool()) View.GONE else View.VISIBLE
        pool_desc.visibility = if(viewModel.isLPool()) View.GONE else View.VISIBLE
        existing_pool_id.visibility = if(viewModel.isLPool()) View.GONE else View.VISIBLE
        pool_registration_continue.isEnabled = getExistingPoolIdText().isNotEmpty() || viewModel.isLPool() || pool_id.getText().isNotEmpty()
        hideError()
    }

    private fun onContinueClicked() {
        continueValidating()
    }

    private fun continueValidating() {
        if (pool_id.length() > 0 || viewModel.isLPool()) {  // If we are L-Pool we do not need a pool id
            KeyboardUtil.hideKeyboard(this)
            if (viewModel.isLPool()) viewModel.setPoolID("") else viewModel.setPoolID(pool_id.text.toString())
        } else {
            viewModel.setPoolID(getExistingPoolIdText())
        }
        viewModel.validatePoolId()
    }

    private fun getExistingPoolIdText(): String {
        viewModel.delegationData.account?.accountDelegation?.delegationTarget?.bakerId?.let {
            return it.toString()
        }
        return ""
    }
}
