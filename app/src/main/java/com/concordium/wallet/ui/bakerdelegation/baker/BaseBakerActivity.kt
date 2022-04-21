package com.concordium.wallet.ui.bakerdelegation.baker

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakerData
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity

abstract class BaseBakerActivity(layout: Int, titleId: Int = R.string.app_name) :
    BaseDelegationBakerActivity(layout, titleId) {

    protected lateinit var viewModel: BakerViewModel

    companion object {
        const val EXTRA_BAKER_DATA = "EXTRA_BAKER_DATA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize(intent.extras?.getSerializable(EXTRA_BAKER_DATA) as BakerData)
        initViews()
    }

    open fun initializeViewModel() {
        showWaiting(false)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(BakerViewModel::class.java)
    }
}
