package com.concordium.wallet.ui.more.dev

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.databinding.ActivityDevBinding
import com.concordium.wallet.ui.base.BaseActivity

class DevActivity : BaseActivity() {
    private lateinit var binding: ActivityDevBinding
    private lateinit var viewModel: DevViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDevBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        )[DevViewModel::class.java]
        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })
    }

    private fun initializeViews() {
        binding.includeProgress.progressLayout.visibility = View.GONE

        binding.createDataButton.setOnClickListener {
            viewModel.createData()
        }

        binding.clearDataButton.setOnClickListener {
            viewModel.clearData()
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }

    //endregion
}
