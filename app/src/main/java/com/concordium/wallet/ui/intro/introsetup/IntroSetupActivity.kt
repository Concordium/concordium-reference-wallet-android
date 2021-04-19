package com.concordium.wallet.ui.intro.introsetup

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.ui.auth.setup.AuthSetupActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.identity.identitycreate.IdentityCreateActivity
import com.concordium.wallet.ui.more.import.ImportActivity
import kotlinx.android.synthetic.main.activity_intro_setup.*

class IntroSetupActivity :
    BaseActivity(R.layout.activity_intro_setup, R.string.intro_setup_title) {


    private lateinit var viewModel: IntroSetupViewModel


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
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(IntroSetupViewModel::class.java)
    }

    private fun initViews() {
        hideActionBarBack(this)

        confirm_create_initial_button.setOnClickListener {
            gotoCreateInitial()
        }
        confirm_import_button.setOnClickListener {
            gotoImport()
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoCreateInitial() {
        finish()
        val intent = Intent(this, IdentityCreateActivity::class.java)
        startActivity(intent)
    }

    private fun gotoImport() {
        finish()
        val intent = Intent(this, ImportActivity::class.java)
        startActivity(intent)
    }

    //endregion

}
