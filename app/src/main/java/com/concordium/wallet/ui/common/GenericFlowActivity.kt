package com.concordium.wallet.ui.common

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.concordium.wallet.R
import com.concordium.wallet.ui.account.accountdetails.WebViewPageFragment
import com.concordium.wallet.ui.base.BaseActivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_intro_flow.*

abstract class GenericFlowActivity(titleId: Int) : BaseActivity(R.layout.activity_intro_flow, titleId) {

    companion object {
        const val EXTRA_HIDE_BACK = "EXTRA_HIDE_BACK"
        const val EXTRA_IGNORE_BACK_PRESS = "EXTRA_IGNORE_BACK_PRESS"
    }

    protected var hideBack = true
    protected var ignoreBackPress = true

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideBack = intent.extras?.getBoolean(EXTRA_HIDE_BACK) == true
        ignoreBackPress = intent.extras?.getBoolean(EXTRA_IGNORE_BACK_PRESS) == true

        initializeViewModel()
        initViews()
    }

    override fun onBackPressed() {
        if(ignoreBackPress){
            // Ignore back press
        }
        else{
            super.onBackPressed()
        }
    }

    // endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
    }

    private fun initViews() {

        pager.adapter =  ScreenSlidePagerAdapter(this)

        TabLayoutMediator(pagers_tab_layout, pager)
        { tab, position ->}.attach()

        create_ident_intro_back.setOnClickListener {
            pager.setCurrentItem(pager.currentItem-1, true)
        }
        create_ident_intro_next.setOnClickListener {
            pager.setCurrentItem(pager.currentItem+1, true)
        }
        create_ident_intro_continue.setOnClickListener{
            gotoContinue()
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

        if(hideBack){
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            toolbar.setNavigationIcon(null);
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        return if (id == android.R.id.home) {
            onBackPressed()
            true
        } else super.onOptionsItemSelected(item)
    }

    abstract fun gotoContinue()

    abstract fun getMaxPages(): Int

    abstract fun getPageTitle(position: Int): Int

    abstract fun getLink(position: Int): String

    //endregion

    //region Control/UI
    //************************************************************


    private fun updateButtons(){

        if(pager.currentItem == 0){
            create_ident_intro_continue.visibility = View.GONE
            create_ident_intro_back.visibility = View.GONE
            create_ident_intro_next.visibility = View.VISIBLE
        }
        if(pager.currentItem > 0 && pager.currentItem < getMaxPages() -1){
            create_ident_intro_continue.visibility = View.GONE
            create_ident_intro_back.visibility = View.VISIBLE
            create_ident_intro_next.visibility = View.VISIBLE
        }
        if(pager.currentItem == getMaxPages() -1){
            create_ident_intro_continue.visibility = View.VISIBLE
            create_ident_intro_back.visibility = View.VISIBLE
            create_ident_intro_next.visibility = View.GONE
        }
    }


    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = getMaxPages()
        override fun createFragment(position: Int): Fragment = WebViewPageFragment(getLink(position), getPageTitle(position))
    }


    //endregion

}
