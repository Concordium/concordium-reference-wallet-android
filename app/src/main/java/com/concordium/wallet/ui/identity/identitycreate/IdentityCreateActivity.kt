package com.concordium.wallet.ui.identity.identitycreate

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.databinding.ActivityIdentityCreateBinding
import com.concordium.wallet.ui.base.BaseActivity

class IdentityCreateActivity : BaseActivity() {
    private lateinit var binding: ActivityIdentityCreateBinding
    private lateinit var viewModel: IdentityCreateViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeViewModel()
        viewModel.initialize()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[IdentityCreateViewModel::class.java]
    }

    //endregion
}
