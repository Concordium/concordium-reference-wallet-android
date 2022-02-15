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
import com.concordium.wallet.ui.base.BaseActivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_shielding_intro.*


class ShieldingIntroActivity :
    BaseActivity(R.layout.activity_shielding_intro, R.string.shielding_intro_title) {

    companion object {
        const val EXTRA_RESULT_SHIELDING_ENABLED = "EXTRA_RESULT_SHIELDING_ENABLED"
        val TITLES = intArrayOf(R.string.shielding_intro_subtitle1,R.string.shielding_intro_subtitle2,R.string.shielding_intro_subtitle3,R.string.shielding_intro_subtitle4,R.string.shielding_intro_subtitle5,R.string.shielding_intro_subtitle6,R.string.shielding_intro_subtitle7)
        const val MAX_PAGES = 7
    }


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pager.adapter = ScreenSlidePagerAdapter(this)

        TabLayoutMediator(pagers_tab_layout, pager)
        { tab, position ->}.attach()

        shielding_intro_skip.setOnClickListener {
            finishSuccessfully()
        }
        shielding_intro_back.setOnClickListener {
            pager.setCurrentItem(pager.currentItem-1, true)
        }
        shielding_intro_next.setOnClickListener {
            pager.setCurrentItem(pager.currentItem+1, true)
        }
        shielding_intro_continue.setOnClickListener{
            finishSuccessfully()
        }

        updateButtons()

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }


            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtons()
            }
        })
    }

    override fun onResume() {
        super.onResume()
    }


    override fun onPause() {
        super.onPause()
    }
    // endregion


    private fun updateButtons(){
        if(pager.currentItem == 0){
            shielding_intro_continue.visibility = View.GONE
            shielding_intro_back.visibility = View.GONE
            shielding_intro_skip.visibility = View.VISIBLE
            shielding_intro_next.visibility = View.VISIBLE
        }
        if(pager.currentItem > 0 && pager.currentItem < MAX_PAGES-1){
            shielding_intro_continue.visibility = View.GONE
            shielding_intro_back.visibility = View.VISIBLE
            shielding_intro_skip.visibility = View.GONE
            shielding_intro_next.visibility = View.VISIBLE
        }
        if(pager.currentItem == MAX_PAGES-1){
            shielding_intro_continue.visibility = View.VISIBLE
            shielding_intro_back.visibility = View.VISIBLE
            shielding_intro_skip.visibility = View.GONE
            shielding_intro_next.visibility = View.GONE
        }
    }

    private fun initializeViewModel() {
    }


    private fun initViews() {
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

