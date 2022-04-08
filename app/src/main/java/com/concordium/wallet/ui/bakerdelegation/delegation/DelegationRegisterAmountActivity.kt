package com.concordium.wallet.ui.bakerdelegation.delegation

import android.app.AlertDialog
import android.content.DialogInterface
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
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.view.SegmentedControlView
import kotlinx.android.synthetic.main.activity_delegation_registration_amount.*
import kotlinx.android.synthetic.main.progress.*


class DelegationRegisterAmountActivity() :
    BaseActivity(R.layout.activity_delegation_registration_amount, R.string.delegation_register_delegation_title) {

    private lateinit var viewModel: DelegationViewModel
    private var reduceWarningDialog: AlertDialog? = null


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
        showWaiting(false)

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
                    showConfirmationPage()
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
        //pool_id.setTextColor(getColor(R.color.text_pink))
        //pool_id_error.visibility = View.VISIBLE
    }

    private fun hideError() {
        //pool_id.setTextColor(getColor(R.color.theme_blue))
        //pool_id_error.visibility = View.GONE
    }

    private fun showConfirmationPage() {

    }

    fun initViews() {
        restake_options.clearAll()
        restake_options.addControl(getString(R.string.delegation_register_delegation_yes_restake), object: SegmentedControlView.OnItemClickListener {
            override fun onItemClicked(){
                viewModel.markRestake(true)
                updateVisibilities()
            }
        }, viewModel.delegationData.restake)
        restake_options.addControl(getString(R.string.delegation_register_delegation_no_restake), object: SegmentedControlView.OnItemClickListener {
            override fun onItemClicked(){
                viewModel.markRestake(false)
                updateVisibilities()
            }
        }, !viewModel.delegationData.restake)

        //amount.setText(viewModel.getPoolId())
        amount.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onContinueClicked()
                    true
                }
                false
            }

        amount.doOnTextChanged { text, start, count, after ->
            //updateVisibilities()
            viewModel.setAmount(CurrencyUtil.toGTUValue(amount.text.toString()))
        }

        pool_registration_continue.setOnClickListener {
            onContinueClicked()
        }



        delegation_amount.text = CurrencyUtil.formatGTU(0, true)
        viewModel.delegationData.account?.let { account ->
            balance_amount.text = CurrencyUtil.formatGTU(account.totalUnshieldedBalance, true)
            account.accountDelegation?.let { accountDelegation ->
                delegation_amount.text = CurrencyUtil.toGTUValue(accountDelegation.stakedAmount)?.let { it -> CurrencyUtil.formatGTU(it) }
            }
        }

        pool_limit.text =
            viewModel.delegationData.bakerPoolStatus?.let {
                CurrencyUtil.toGTUValue(it.delegatedCapitalCap)
                    ?.let { CurrencyUtil.formatGTU(it, true) }
            }
        current_pool.text =
            viewModel.delegationData.bakerPoolStatus?.let {
                CurrencyUtil.toGTUValue(it.delegatedCapital)
                    ?.let { CurrencyUtil.formatGTU(it, true) }
            }

        viewModel.transactionFeeLiveData.observe(this, object : Observer<Long> {
            override fun onChanged(value: Long?) {
                value?.let {
                    pool_estimated_transaction_fee.visibility = View.VISIBLE
                    pool_estimated_transaction_fee.text = getString(
                        R.string.delegation_register_delegation_amount_estimated_transaction_fee, CurrencyUtil.formatGTU(value)
                    )
                    //updateConfirmButton()
                }
            }
        })

        pool_info.visibility = if(viewModel.isLPool()) View.VISIBLE else View.GONE

        viewModel.loadTransactionFee()

        updateVisibilities()

        updateContent()
    }

    private fun updateContent() {
        if(viewModel.delegationData.type == DelegationData.TYPE_UPDATE_DELEGATION){
            amount_desc.setText(getString(R.string.delegation_update_delegation_amount_enter_amount))
            amount.setText(viewModel.delegationData.amount?.let { CurrencyUtil.formatGTU(it,false) })
        }
    }

    private fun updateVisibilities() {
        //pool_id.visibility = if(viewModel.isLPool()) View.GONE else View.VISIBLE
        //pool_desc.visibility = if(viewModel.isLPool()) View.GONE else View.VISIBLE
        //pool_registration_continue.isEnabled = pool_id.length() > 0
        hideError()
    }

    private fun onContinueClicked() {

        //TODO proper validity testing here
        //Align with iOS code here and Figma flow
        /*

        struct StakeAmountInputValidator {
            var minimumValue: GTU
            var maximumValue: GTU
            var atDisposal: GTU
            var currentPool: GTU?
            var poolLimit: GTU?

            func validate(amount: GTU) -> AnyPublisher<GTU, StakeError> {
                .just(amount).flatMap {
                    checkMaximum(amount: $0)
                }.flatMap {
                    checkMinimum(amount: $0)
                }.flatMap {
                    checkAtDisposal(amount: $0)
                }.flatMap {
                    checkPoolLimit(amount: $0)
                }.eraseToAnyPublisher()

            }

            func checkMaximum(amount: GTU) -> AnyPublisher<GTU, StakeError> {
                if amount.intValue > maximumValue.intValue {
                    return .fail(.maximumAmount(maximumValue))
                }
                return .just(amount)
            }
            func checkMinimum(amount: GTU) -> AnyPublisher<GTU, StakeError> {
                if amount.intValue < minimumValue.intValue {
                    return .fail(.minimumAmount(minimumValue))
                }
                return .just(amount)
            }
            func checkAtDisposal(amount:GTU) -> AnyPublisher<GTU, StakeError> {
                if amount.intValue > atDisposal.intValue {
                    return .fail(.notEnoughFund(atDisposal))
                }
                return .just(amount)
            }
            func checkPoolLimit(amount: GTU) -> AnyPublisher<GTU, StakeError> {
                guard let currentPool = currentPool, let poolLimit = poolLimit else {
                    return .just(amount)
                }
                if amount.intValue + currentPool.intValue > poolLimit.intValue {
                    return .fail(.poolLimitReached(currentPool, poolLimit))
                }
                return .just(amount)
            }
        }
        */

        //Replace this flow for validation...
        val amounttoStake = amount.text.toString()

        if(amounttoStake.isEmpty()){
            //Show error
        }
        else{
            if(viewModel.delegationData.amount == null){
                continueToConfirmation()
            }
            else{
                if(amounttoStake.toLong() < viewModel.delegationData.amount!!){
                    showReduceWarning()
                }
                else{
                    continueToConfirmation()
                }
            }
        }
    }

    private fun continueToConfirmation() {
        val intent = Intent(this, DelegationRegisterConfirmationActivity::class.java)
        viewModel.delegationData.amount = CurrencyUtil.toGTUValue(amount.text.toString())
        intent.putExtra(DelegationRegisterConfirmationActivity.EXTRA_DELEGATION_DATA, viewModel.delegationData)
        startActivityForResultAndHistoryCheck(intent)
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

    private fun showReduceWarning() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.delegation_register_delegation_reduce_warning_title))
        builder.setMessage(getString(R.string.delegation_register_delegation_reduce_warning_content))
        builder.setNegativeButton(
            getString(R.string.delegation_register_delegation_reduce_warning_cancel),
            object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    reduceWarningDialog?.dismiss()
                }
            })
        builder.setPositiveButton(
            getString(R.string.delegation_register_delegation_reduce_warning_ok),
            object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    continueToConfirmation()
                }
            })
        builder.setCancelable(true)
        reduceWarningDialog = builder.create()//.show()
        reduceWarningDialog?.show()

    }

}
