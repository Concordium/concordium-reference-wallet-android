package com.concordium.wallet.ui.intro.introstart

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.ui.auth.setup.AuthSetupActivity
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_intro_start.*

class IntroStartActivity : BaseActivity(R.layout.activity_intro_start, R.string.intro_start_title) {

    private lateinit var viewModel: IntroStartViewModel


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        ).get(IntroStartViewModel::class.java)
    }

    private fun initViews() {
        confirm_button.setOnClickListener {
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
