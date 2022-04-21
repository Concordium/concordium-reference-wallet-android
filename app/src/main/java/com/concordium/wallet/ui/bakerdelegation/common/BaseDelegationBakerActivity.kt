package com.concordium.wallet.ui.bakerdelegation.common

import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.progress.*

abstract class BaseDelegationBakerActivity(layout: Int, titleId: Int = R.string.app_name) :
    BaseActivity(layout, titleId) {

    protected open fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    protected open fun initViews() { }
}
