package com.concordium.wallet.ui.passphrase.setup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivitySetupWalletBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.identity.identitycreate.IdentityIntroFlow
import com.concordium.wallet.util.KeyboardUtil

class SetupWalletActivity : BaseActivity(), AuthDelegate by AuthDelegateImpl() {
    private lateinit var binding: ActivitySetupWalletBinding
    private lateinit var viewModel: PassPhraseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.pass_phrase_title
        )
        initializeViewModel()
        initViews()
        initObservers()

        if (BuildConfig.DEBUG) {
            binding.toolbarLayout.toolbarTitle.isClickable = true
            binding.toolbarLayout.toolbarTitle.setOnClickListener {
                viewModel.hack()
            }
        }
    }

    override fun onBackPressed() {
        if (binding.pager.currentItem != 3)
            super.onBackPressed()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[PassPhraseViewModel::class.java]
    }

    private fun initViews() {
        binding.pager.adapter = ScreenSlidePagerAdapter(this)

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 2)
                    binding.continueButton.visibility = View.GONE
                else
                    binding.continueButton.visibility = View.VISIBLE
            }
        })

        binding.pager.isUserInputEnabled = false

        binding.continueButton.setOnClickListener {
            if (binding.pager.currentItem == (binding.pager.adapter as ScreenSlidePagerAdapter).itemCount - 1) {
                finish()
                startActivity(Intent(this, IdentityIntroFlow::class.java))
            } else {
                moveNext()
            }
        }
    }

    private fun initObservers() {
        viewModel.validate.observe(this) { success ->
            if (success) {
                showAuthentication(activity = this, authenticated = { password ->
                    password?.let {
                        viewModel.setSeedPhrase(password)
                    }
                })
            }
        }

        viewModel.saveSeed.observe(this) { saveSuccess ->
            if (saveSuccess) {
                moveNext()
            } else {
                KeyboardUtil.hideKeyboard(this)
                showError(R.string.auth_login_seed_error)
            }
        }

        viewModel.reveal.observe(this) { success ->
            if (success) moveNext()
        }

        viewModel.continueEnabled.observe(this) { enabled ->
            binding.continueButton.isEnabled = enabled
        }
    }

    private inner class ScreenSlidePagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> PassPhraseExplainFragment()
                1 -> PassPhraseRevealedFragment.newInstance(viewModel)
                2 -> PassPhraseInputFragment.newInstance(viewModel)
                3 -> PassPhraseSuccessFragment()
                else -> Fragment()
            }
        }
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(binding.root, stringRes)
    }

    private fun moveNext() {
        binding.pager.currentItem++
        when (binding.pager.currentItem) {
            1 -> binding.continueButton.isEnabled = false
            2 -> binding.progressLine.setFilledDots(2)
            3 -> hideActionBarBack()
        }
    }
}
