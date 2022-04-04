package com.concordium.wallet.ui.intro.introflow

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.account.accountdetails.WebViewPageFragment
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.identity.identitycreate.IdentityCreateActivity
import com.concordium.wallet.ui.more.import.ImportActivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_intro_flow.*

class IntroFlowActivity :
    BaseActivity(R.layout.activity_intro_flow, R.string.intro_flow_title) {

    companion object {
        const val EXTRA_HIDE_BACK = "EXTRA_HIDE_BACK"
        val TITLES = intArrayOf(R.string.intro_start_create_intro_subtitle1,R.string.intro_start_create_intro_subtitle2,R.string.intro_start_create_intro_subtitle3,R.string.intro_start_create_intro_subtitle4,R.string.intro_start_create_intro_subtitle5)
        const val MAX_PAGES = 5
    }

    private var hideBack = true

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideBack = intent.extras?.getBoolean(EXTRA_HIDE_BACK) == true

        initializeViewModel()
        initViews()
    }

    override fun onBackPressed() {
        // Ignore back press
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
            gotoAccountName()
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

    private fun gotoAccountName() {
        //finish()
        val intent = Intent(this, IdentityCreateActivity::class.java)
        startActivity(intent)
    }

    //endregion

    //region Control/UI
    //************************************************************


    private fun updateButtons(){

        if(pager.currentItem == 0){
            create_ident_intro_continue.visibility = View.GONE
            create_ident_intro_back.visibility = View.GONE
            create_ident_intro_next.visibility = View.VISIBLE
        }
        if(pager.currentItem > 0 && pager.currentItem < MAX_PAGES -1){
            create_ident_intro_continue.visibility = View.GONE
            create_ident_intro_back.visibility = View.VISIBLE
            create_ident_intro_next.visibility = View.VISIBLE
        }
        if(pager.currentItem == MAX_PAGES -1){
            create_ident_intro_continue.visibility = View.VISIBLE
            create_ident_intro_back.visibility = View.VISIBLE
            create_ident_intro_next.visibility = View.GONE
        }
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = MAX_PAGES //There are MAX_PAGES html pages enumerated in title
        override fun createFragment(position: Int): Fragment = WebViewPageFragment("file:///android_asset/intro_flow_onboarding_en_"+(position+1)+".html", TITLES[position])
    }

    //endregion

}
