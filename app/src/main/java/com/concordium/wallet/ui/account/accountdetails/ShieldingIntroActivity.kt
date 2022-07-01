package com.concordium.wallet.ui.account.accountdetails

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityShieldingIntroBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.google.android.material.tabs.TabLayoutMediator

class ShieldingIntroActivity : BaseActivity() {
    companion object {
        const val EXTRA_RESULT_SHIELDING_ENABLED = "EXTRA_RESULT_SHIELDING_ENABLED"
        val TITLES = intArrayOf(R.string.shielding_intro_subtitle1,R.string.shielding_intro_subtitle2,R.string.shielding_intro_subtitle3,R.string.shielding_intro_subtitle4,R.string.shielding_intro_subtitle5,R.string.shielding_intro_subtitle6,R.string.shielding_intro_subtitle7)
        const val MAX_PAGES = 7
    }

    private lateinit var binding: ActivityShieldingIntroBinding

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShieldingIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.shielding_intro_title)

        binding.pager.adapter = ScreenSlidePagerAdapter(this)

        TabLayoutMediator(binding.pagersTabLayout, binding.pager)
        { tab, position ->}.attach()

        binding.shieldingIntroSkip.setOnClickListener {
            finishSuccessfully()
        }
        binding.shieldingIntroBack.setOnClickListener {
            binding.pager.setCurrentItem(binding.pager.currentItem-1, true)
        }
        binding.shieldingIntroNext.setOnClickListener {
            binding.pager.setCurrentItem(binding.pager.currentItem+1, true)
        }
        binding.shieldingIntroContinue.setOnClickListener{
            finishSuccessfully()
        }

        updateButtons()

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtons()
            }
        })
    }

    // endregion

    private fun updateButtons(){
        if (binding.pager.currentItem == 0){
            binding.shieldingIntroContinue.visibility = View.GONE
            binding.shieldingIntroBack.visibility = View.GONE
            binding.shieldingIntroSkip.visibility = View.VISIBLE
            binding.shieldingIntroNext.visibility = View.VISIBLE
        }
        if (binding.pager.currentItem > 0 && binding.pager.currentItem < MAX_PAGES-1){
            binding.shieldingIntroContinue.visibility = View.GONE
            binding.shieldingIntroBack.visibility = View.VISIBLE
            binding.shieldingIntroSkip.visibility = View.GONE
            binding.shieldingIntroNext.visibility = View.VISIBLE
        }
        if (binding.pager.currentItem == MAX_PAGES-1){
            binding.shieldingIntroContinue.visibility = View.VISIBLE
            binding.shieldingIntroBack.visibility = View.VISIBLE
            binding.shieldingIntroSkip.visibility = View.GONE
            binding.shieldingIntroNext.visibility = View.GONE
        }
    }

    private fun finishSuccessfully(){
        val intent = Intent()
        intent.putExtra(EXTRA_RESULT_SHIELDING_ENABLED, true)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = MAX_PAGES //There are MAX_PAGES html pages enumerated in title
        override fun createFragment(position: Int): Fragment = WebViewPageFragment("file:///android_asset/schielded_balance_flow_en_"+(position+1)+".html", TITLES[position])
    }
}
