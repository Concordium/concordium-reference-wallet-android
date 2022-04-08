package com.concordium.wallet.ui.bakerdelegation.common

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.delegationbaker_status.*

abstract class StatusActivity(titleId: Int) :
    BaseActivity(R.layout.delegationbaker_status, titleId) {

    fun setContentTitle(res: Int){
        status_title.text = getString(res)
    }

    fun addContent(titleRes: Int, text: String) {
        addContent(getString(titleRes), text)
    }

    fun addContent(title: String, text: String) {
        status_empty.visibility = View.GONE
        status_list_container.visibility = View.VISIBLE

        val view = LayoutInflater.from(this).inflate(R.layout.delegation_baker_status_content_item, null)

        if (title.isNotEmpty())
            view.findViewById<TextView>(R.id.status_item_title).text = title
        else
            view.findViewById<TextView>(R.id.status_item_title).visibility = View.GONE

        if (text.isNotEmpty())
            view.findViewById<TextView>(R.id.status_item_content).text = text
        else
            view.findViewById<TextView>(R.id.status_item_content).visibility = View.GONE

        status_list_container.addView(view)
    }

    fun setEmptyState(text: String){
        status_empty.text = text
        status_empty.visibility = View.VISIBLE
        status_list_container.visibility = View.GONE
    }

    fun clearState(){
        status_empty.text = ""
        status_list_container.removeAllViews()
    }

    abstract fun initView()
}
