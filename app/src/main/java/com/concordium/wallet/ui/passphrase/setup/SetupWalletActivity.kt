package com.concordium.wallet.ui.passphrase.setup

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivitySetupWalletBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.google.android.material.tabs.TabLayoutMediator

class SetupWalletActivity : BaseActivity() {
    private lateinit var binding: ActivitySetupWalletBinding
    private lateinit var viewModel: PassPhraseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.pass_phrase_title)
        initializeViewModel()
        initViews()
        initObservers()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[PassPhraseViewModel::class.java]
    }

    private fun initViews() {
        binding.pager.adapter = ScreenSlidePagerAdapter(this)

        TabLayoutMediator(binding.pagersTabLayout, binding.pager) { _, _ -> }.attach()

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 3)
                    binding.continueButton.visibility = View.GONE
                else
                    binding.continueButton.visibility = View.VISIBLE
            }
        })

        binding.pager.isUserInputEnabled = false

        binding.continueButton.setOnClickListener {
            if (binding.pager.currentItem == (binding.pager.adapter as ScreenSlidePagerAdapter).itemCount - 1) {
                println("LC -> Must go to explain create identity flow")
                finish()
                startActivity(Intent(this, MainActivity::class.java))
                TODO("Must go to recover accounts and identities flow!")
            } else {
                moveNext()
            }
        }
    }

    private fun initObservers() {
        viewModel.validate.observe(this) { success ->
            if (success) moveNext()
        }

        viewModel.reveal.observe(this) { success ->
            if (success) moveNext()
        }
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> PassPhraseExplainFragment()
                1 -> PassPhraseHiddenFragment.newInstance(viewModel)
                2 -> PassPhraseRevealedFragment.newInstance(viewModel)
                3 -> PassPhraseInputFragment.newInstance(viewModel)
                4 -> PassPhraseSuccessFragment()
                else -> Fragment()
            }
        }
    }

    private fun moveNext() {
        if (binding.pager.currentItem != 2 || viewModel.passPhraseConfirmChecked)
            binding.pager.currentItem++
    }
}
