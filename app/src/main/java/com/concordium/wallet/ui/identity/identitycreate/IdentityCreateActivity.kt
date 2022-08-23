package com.concordium.wallet.ui.identity.identitycreate

import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityIdentityCreateBinding
import com.concordium.wallet.ui.base.BaseActivity

class IdentityCreateActivity : BaseActivity() {
    private lateinit var binding: ActivityIdentityCreateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeViews()
    }

    private fun initializeViews() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, IdentityCreateIdentityNameFragment())
            .commit()
    }
}
