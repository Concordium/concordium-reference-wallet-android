package com.concordium.wallet.ui.identity.identitycreate

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseActivity

class IdentityCreateActivity :
    BaseActivity(R.layout.activity_identity_create) {

    private lateinit var viewModel: IdentityCreateViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initViews()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(IdentityCreateViewModel::class.java)
    }

    private fun initViews() {

    }

    //endregion

    //region Control/UI
    //************************************************************

    //endregion
}
