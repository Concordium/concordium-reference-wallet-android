package com.concordium.wallet.ui.passphrase.recover

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityRecoverWalletBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.passphrase.recoverprocess.RecoverProcessActivity

class RecoverWalletActivity : BaseActivity() {
    private lateinit var binding: ActivityRecoverWalletBinding
    private lateinit var viewModel: PassPhraseRecoverViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoverWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.pass_phrase_recover_title)
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

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[PassPhraseRecoverViewModel::class.java]
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
                startActivity(Intent(this, RecoverProcessActivity::class.java))
            } else {
                binding.pager.currentItem++
            }
        }
    }

    private fun initObservers() {
        viewModel.validate.observe(this) { success ->
            if (success)
                binding.pager.currentItem++
        }
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 4
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> PassPhraseRecoverExplain1Fragment()
                1 -> PassPhraseRecoverExplain2Fragment()
                2 -> PassPhraseRecoverInputFragment.newInstance(viewModel)
                3 -> PassPhraseRecoverSuccessFragment()
                else -> Fragment()
            }
        }
    }
}
