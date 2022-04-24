package com.concordium.wallet.ui.bakerdelegation.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.DelegationData
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.delegationbaker_status.*

abstract class StatusActivity(titleId: Int) :
    BaseActivity(R.layout.delegationbaker_status, titleId) {

    protected lateinit var viewModel: DelegationBakerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize(intent.extras?.getSerializable(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA) as DelegationData)
        initView()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(DelegationBakerViewModel::class.java)
    }

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
