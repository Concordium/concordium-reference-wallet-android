package com.concordium.wallet.ui.bakerdelegation.delegation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.DelegationData
import com.concordium.wallet.data.model.DelegationTarget
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.view.SegmentedControlView
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.activity_delegation_registration_pool.*
import kotlinx.android.synthetic.main.progress.*

class DelegationRegisterPoolActivity() :
    BaseActivity(R.layout.activity_delegation_registration_pool, R.string.delegation_register_delegation_title) {

    private lateinit var lPoolControl: View
    private lateinit var bakerPoolControl: View

    private lateinit var viewModel: DelegationViewModel

    companion object {
        const val EXTRA_DELEGATION_DATA = "EXTRA_DELEGATION_DATA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize(intent.extras?.getSerializable(EXTRA_DELEGATION_DATA) as DelegationData)
        initViews()
    }


    fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(DelegationViewModel::class.java)

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })

        viewModel.showDetailedLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showDetailedPage()
                }
            }
        })

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError()
            }
        })
    }

    private fun showError() {
        pool_id.setTextColor(getColor(R.color.text_pink))
        pool_id_error.visibility = View.VISIBLE
    }

    private fun hideError() {
        pool_id.setTextColor(getColor(R.color.theme_blue))
        pool_id_error.visibility = View.GONE
    }

    private fun showDetailedPage() {
        val intent = Intent(this, DelegationRegisterAmountActivity::class.java)
        intent.putExtra(DelegationRegisterAmountActivity.EXTRA_DELEGATION_DATA, viewModel.delegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    fun initViews() {
        showWaiting(false)
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

    private fun updateContent() {
        if(viewModel.delegationData.type == DelegationData.TYPE_UPDATE_DELEGATION){
            var existingPoolIdText = viewModel.delegationData.account?.accountDelegation?.delegationTarget?.bakerId.toString()
            existing_pool_id.text = getString(R.string.delegation_update_delegation_pool_id, existingPoolIdText)
            existing_pool_id.visibility = View.VISIBLE
            pool_id.setText(existingPoolIdText)
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
        }
        else{
            existing_pool_id.visibility = View.GONE
        }
    }

    private fun updateVisibilities() {
        pool_id.visibility = if(viewModel.isLPool()) View.GONE else View.VISIBLE
        pool_desc.visibility = if(viewModel.isLPool()) View.GONE else View.VISIBLE
        pool_registration_continue.isEnabled = pool_id.length() > 0 || viewModel.isLPool()
        hideError()
    }

    private fun onContinueClicked() {
        continueValidating()
    }

    private fun continueValidating() {
        if(pool_id.length() > 0 || viewModel.isLPool()){  //If we are L-Pool we do not need a pool id
            KeyboardUtil.hideKeyboard(this)
            viewModel.setPoolID(pool_id.text.toString())
            viewModel.validatePoolId()
        }
    }


    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
            pool_registration_continue.isEnabled = false
        } else {
            progress_layout.visibility = View.GONE
            pool_registration_continue.isEnabled = true
        }
    }


}
