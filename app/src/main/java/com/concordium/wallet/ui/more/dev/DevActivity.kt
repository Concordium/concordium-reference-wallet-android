package com.concordium.wallet.ui.more.dev

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_dev.*
import kotlinx.android.synthetic.main.progress.*

class DevActivity : BaseActivity(R.layout.activity_dev, R.string.app_name) {

    private lateinit var viewModel: DevViewModel


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(DevViewModel::class.java)

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })
    }

    private fun initializeViews() {
        progress_layout.visibility = View.GONE

        create_data_button.setOnClickListener {
            viewModel.createData()
        }

        clear_data_button.setOnClickListener {
            viewModel.clearData()
        }

    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    //endregion

}