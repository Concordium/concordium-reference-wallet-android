package com.concordium.wallet.ui.bakerdelegation.common

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.DelegationData
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.progress.*

abstract class BaseDelegationBakerActivity(layout: Int, titleId: Int = R.string.app_name) :
    BaseActivity(layout, titleId) {

    protected lateinit var viewModel: DelegationBakerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize(intent.extras?.getSerializable(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA) as DelegationData)
        initViews()
    }

    open fun initializeViewModel() {
        showWaiting(false)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(DelegationBakerViewModel::class.java)
    }

    protected open fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    protected open fun initViews() { }
}
