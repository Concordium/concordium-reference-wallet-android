package com.concordium.wallet.ui.intro.introsetup

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityIntroSetupBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.ui.intro.introflow.IntroFlowActivity
import com.concordium.wallet.ui.more.import.ImportActivity

class IntroSetupActivity : BaseActivity() {
    private lateinit var binding: ActivityIntroSetupBinding
    private lateinit var viewModel: IntroSetupViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.intro_setup_title)

        initializeViewModel()
        viewModel.initialize()
        initViews()
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    // endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[IntroSetupViewModel::class.java]
    }

    private fun initViews() {
        hideActionBarBack(this)

        binding.confirmCreateInitialButton.setOnClickListener {
            gotoCreateInitial()
        }
        binding.confirmImportButton.setOnClickListener {
            gotoImport()
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoCreateInitial() {
        finish()
        val intent = Intent(this, IntroFlowActivity::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_HIDE_BACK, true)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, true)
        startActivity(intent)
    }

    private fun gotoImport() {
        finish()
        val intent = Intent(this, ImportActivity::class.java)
        startActivity(intent)
    }

    //endregion
}
