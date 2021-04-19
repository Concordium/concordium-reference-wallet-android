package com.concordium.wallet.ui.base

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.uicore.popup.Popup

open class BaseFragment(private val titleId: Int? = null) : Fragment() {

    private var titleView: TextView? = null
    protected lateinit var popup: Popup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        popup = Popup()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        if (toolbar != null) {
            (activity as AppCompatActivity).setSupportActionBar(toolbar)
            titleView = toolbar.findViewById<TextView>(R.id.toolbar_title)
            setupActionBar((activity as AppCompatActivity), titleId)
            toolbar.setNavigationOnClickListener {
                (activity as AppCompatActivity).onBackPressed();
            }
        }
    }

    protected fun setupActionBar(activity: AppCompatActivity, titleId: Int?) {
        val actionbar = activity.supportActionBar ?: return
        actionbar.setDisplayHomeAsUpEnabled(true)

        if (titleId != null) {
            actionbar.setTitle(titleId)
            titleView?.setText(titleId)
        }


    }


}