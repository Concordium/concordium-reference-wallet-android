package com.concordium.wallet.ui.passphrase.recover

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityRecoverWalletBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.passphrase.recoverprocess.RecoverProcessActivity
import com.concordium.wallet.util.KeyboardUtil
import org.koin.androidx.viewmodel.ext.android.viewModel

class RecoverWalletActivity : BaseActivity(), AuthDelegate by AuthDelegateImpl() {
    lateinit var binding: ActivityRecoverWalletBinding
    val viewModel: PassPhraseRecoverViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoverWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.pass_phrase_recover_title
        )
        initViews()
        initObservers()

        if (BuildConfig.DEBUG) {
            binding.toolbarLayout.toolbarTitle.isClickable = true
            binding.toolbarLayout.toolbarTitle.setOnClickListener {
                showAuthentication(activity = this, authenticated = { password ->
                    password?.let {
                        viewModel.setPredefinedPhraseForTesting(it)
                    }
                })
            }
        }
    }

    override fun onBackPressed() {
        if (binding.pager.currentItem != 2)
            super.onBackPressed()
    }

    private fun initViews() {
        binding.pager.adapter = ScreenSlidePagerAdapter(this)

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 1)
                    binding.continueButton.visibility = View.GONE
                else
                    binding.continueButton.visibility = View.VISIBLE
                if (position == 2) {
                    hideActionBarBack()
                } else {
                    showActionBarBack()
                }
            }
        })

        binding.pager.isUserInputEnabled = false

        binding.continueButton.setOnClickListener {
            if (binding.pager.currentItem == (binding.pager.adapter as ScreenSlidePagerAdapter).itemCount - 1) {
                finish()
                startActivity(Intent(this, RecoverProcessActivity::class.java))
            } else {
                binding.pager.currentItem++
            }
        }
    }

    private fun initObservers() {
        viewModel.seed.observe(this) { seed ->
            showAuthentication(activity = this, authenticated = { password ->
                password?.let {
                    viewModel.setSeedPhrase(seed, password)
                }
            })
        }
        viewModel.saveSeed.observe(this) { saveSuccess ->
            if (saveSuccess) {
                binding.pager.currentItem++
            } else {
                KeyboardUtil.hideKeyboard(this)
                showError(R.string.auth_login_seed_error)
            }
        }
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(binding.root, stringRes)
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> PassPhraseRecoverExplainFragment()
                1 -> PassPhraseRecoverInputFragment()
                2 -> PassPhraseRecoverSuccessFragment()
                else -> Fragment()
            }
        }
    }
}
