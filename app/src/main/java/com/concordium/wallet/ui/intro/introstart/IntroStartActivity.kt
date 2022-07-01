package com.concordium.wallet.ui.intro.introstart

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.databinding.ActivityIntroStartBinding
import com.concordium.wallet.ui.auth.setup.AuthSetupActivity
import com.concordium.wallet.ui.base.BaseActivity

class IntroStartActivity : BaseActivity() {
    private lateinit var binding: ActivityIntroStartBinding
    private lateinit var viewModel: IntroStartViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        viewModel = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[IntroStartViewModel::class.java]
    }

    private fun initViews() {
        binding.confirmButton.setOnClickListener {
            gotoAuthSetup()
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoAuthSetup() {
        finish()
        val intent = Intent(this, AuthSetupActivity::class.java)
        startActivity(intent)
    }

    override fun loggedOut() {
        // do nothing as we are one of the root activities
    }

    //endregion
}
