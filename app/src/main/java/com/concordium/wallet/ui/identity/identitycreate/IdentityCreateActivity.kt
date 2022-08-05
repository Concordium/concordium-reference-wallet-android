package com.concordium.wallet.ui.identity.identitycreate

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityIdentityCreateBinding
import com.concordium.wallet.ui.base.BaseActivity

class IdentityCreateActivity : BaseActivity() {
    private lateinit var binding: ActivityIdentityCreateBinding
    private lateinit var viewModel: IdentityCreateViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeViewModel()
        initializeViews()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[IdentityCreateViewModel::class.java]
    }

    private fun initializeViews() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, IdentityCreateIdentityNameFragment())
            .commit()
    }
}
