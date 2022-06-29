package com.concordium.wallet.ui.base

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.concordium.wallet.uicore.popup.Popup

open class BaseFragment(private val titleId: Int? = null) : Fragment() {
    protected lateinit var popup: Popup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        popup = Popup()
    }

    protected fun setupToolbar(toolbar: Toolbar, toolbarTitle: TextView) {
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        if (titleId != null) {
            toolbarTitle.setText(titleId)
        }
        setupActionBar((activity as AppCompatActivity), titleId)
        toolbar.setNavigationOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }
    }

    private fun setupActionBar(activity: AppCompatActivity, titleId: Int?) {
        val actionbar = activity.supportActionBar ?: return
        actionbar.setDisplayHomeAsUpEnabled(true)
        if (titleId != null) {
            actionbar.setTitle(titleId)
        }
    }
}
